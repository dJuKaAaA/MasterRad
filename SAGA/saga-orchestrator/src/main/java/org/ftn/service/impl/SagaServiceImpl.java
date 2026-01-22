package org.ftn.service.impl;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.ftn.client.InventoryClient;
import org.ftn.client.OrderClient;
import org.ftn.client.PaymentClient;
import org.ftn.client.dto.*;
import org.ftn.constant.SagaState;
import org.ftn.constant.TransactionState;
import org.ftn.dto.CreateOrderRequestDto;
import org.ftn.dto.SagaResponseDto;
import org.ftn.entity.RecoveryDataEntity;
import org.ftn.entity.SagaEntity;
import org.ftn.mapper.SagaMapper;
import org.ftn.repository.SagaRepository;
import org.ftn.service.SagaService;
import org.ftn.utils.RollbackAction;
import org.ftn.utils.ServiceAccountTokenProvider;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class SagaServiceImpl implements SagaService {
    private final SagaRepository sagaRepository;
    private final SagaMapper sagaMapper;
    private final OrderClient orderClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final ServiceAccountTokenProvider tokenProvider;
    private final ManagedExecutor managedExecutor;

    private static final Logger LOG = Logger.getLogger(SagaServiceImpl.class);

    @Inject
    public SagaServiceImpl(SagaRepository sagaRepository,
                           SagaMapper sagaMapper,
                           @RestClient OrderClient orderClient,
                           @RestClient InventoryClient inventoryClient,
                           @RestClient PaymentClient paymentClient,
                           ServiceAccountTokenProvider tokenProvider,
                           ManagedExecutor managedExecutor) {
        this.sagaRepository = sagaRepository;
        this.sagaMapper = sagaMapper;
        this.orderClient = orderClient;
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
        this.tokenProvider = tokenProvider;
        this.managedExecutor = managedExecutor;
    }

    @Startup
    public void recoverTransactions() {
        Collection<SagaEntity> sagas = sagaRepository
                .find("state in ?1", (Set.of(
                        SagaState.STARTED,
                        SagaState.ORDER_CREATED,
                        SagaState.INVENTORY_RESERVED,
                        SagaState.PAYMENT_COMPLETED)))
                .list();
        if (sagas.isEmpty())
            return;

        LOG.info("Starting recovery");

        for (SagaEntity saga : sagas) {
            if (saga.getTransactionState() == TransactionState.ROLLING_BACK) {
                // Note: During rollback, if an ID is missing, we use UUID.randomUUID().
                // This is intentional: it signals that the service never participated,
                // so the rollback call will be a no-op.
                LOG.infof("Executing roll back for saga %s", saga.getId());
                LinkedList<RollbackAction> rollbackActions = getRollbackActionsForRecovery(saga);
                rollback(
                        saga,
                        rollbackActions,
                        saga.getRecoveryData().getErrorMessage(),
                        saga.getRecoveryData().getErrorStatus()
                );
                LOG.infof("Successful rollback for saga %s", saga.getId());
            } else {
                LOG.infof("Continuing transaction for saga %s", saga.getId());
                OrderResponseDto orderResponseDto;
                InventoryResponseDto inventoryResponseDto = null;
                PaymentResponseDto paymentResponseDto;
                LinkedList<RollbackAction> rollbackActions = new LinkedList<>();
                try {
                    if (saga.getState() == SagaState.STARTED) {
                        orderResponseDto = recoverOrder(saga, rollbackActions);
                        LOG.infof("Created order %s for saga %s", orderResponseDto.id(), saga.getId());
                    }
                    if (saga.getState() == SagaState.ORDER_CREATED) {
                        inventoryResponseDto = recoverInventory(saga, rollbackActions);
                        LOG.infof("Reserved product %s for saga %s", inventoryResponseDto.product().id(), saga.getId());
                    }
                    if (saga.getState() == SagaState.INVENTORY_RESERVED) {
                        BigDecimal price = inventoryResponseDto == null ?
                                saga.getRecoveryData().getPrice() : inventoryResponseDto.product().price();
                        paymentResponseDto = recoverPayment(saga, rollbackActions, price);
                        LOG.infof("Processed payment %s for saga %s", paymentResponseDto.id(), saga.getId());
                    }
                    if (saga.getState() == SagaState.PAYMENT_COMPLETED) {
                        commit(saga);
                        LOG.infof("Successfully commited transaction for saga %s", saga.getId());
                    }
                } catch (WebApplicationException e) {
                    LOG.errorf("Transaction for saga %s failed! Rolling back...", saga.getId());
                    rollback(saga, rollbackActions, e.getMessage(), e.getResponse().getStatus());
                    LOG.infof("Successful rollback for saga %s", saga.getId());
                }
            }
        }

        LOG.info("Successful recovery");
    }

    @Transactional
    @Override
    public void createOrderTransaction(UUID sagaId, UUID idempotencyKey, CreateOrderRequestDto createOrderRequestDto, UUID userId) {
        Optional<SagaEntity> optionalSaga = sagaRepository.findByIdOptional(sagaId);
        SagaEntity saga;
        saga = optionalSaga.orElseGet(() -> start(idempotencyKey));

        OrderResponseDto orderResponseDto = null;
        InventoryResponseDto inventoryResponseDto = null;
        PaymentResponseDto paymentResponseDto = null;
        LOG.infof("Starting saga %s", saga.getId());

        LinkedList<RollbackAction> rollbackActions = new LinkedList<>();
        try {
            saga.getRecoveryData().setProductId(createOrderRequestDto.productId());
            saga.getRecoveryData().setAmount(createOrderRequestDto.amount());
            saga.getRecoveryData().setUserId(userId);
            sagaRepository.persistAndFlush(saga);

            // Step 1: Create order
            orderResponseDto = createOrder(
                    saga,
                    new OrderRequestDto(
                            createOrderRequestDto.productId(),
                            createOrderRequestDto.amount(),
                            userId
                    )
            );
            final UUID orderId = orderResponseDto.id();
            rollbackActions.add(() -> orderClient.cancelOrder(orderId, tokenProvider.getAccessToken()));
            LOG.infof("Created order %s for saga %s", orderResponseDto.id(), saga.getId());

            // Step 2: Reserve resources
            inventoryResponseDto = reserveInventory(
                    saga,
                    createOrderRequestDto.productId(),
                    createOrderRequestDto.amount()
            );
            final UUID productId = inventoryResponseDto.product().id();
            rollbackActions.add(() -> inventoryClient.release(productId, createOrderRequestDto.amount(), tokenProvider.getAccessToken()));
            LOG.infof("Reserved product %s for saga %s", inventoryResponseDto.product().id(), saga.getId());

            // Step 3: Process payment
            paymentResponseDto = processPayment(
                    saga,
                    new PaymentRequestDto(
                            inventoryResponseDto.product().price(),
                            createOrderRequestDto.productId(),
                            createOrderRequestDto.amount(),
                            userId
                    )
            );
            final UUID paymentId = paymentResponseDto.id();
            rollbackActions.add(() -> paymentClient.refund(paymentId, tokenProvider.getAccessToken()));
            LOG.infof("Processed payment %s for saga %s", paymentResponseDto.id(), saga.getId());

            commit(saga);
            LOG.infof("Successfully commited transaction for saga %s", saga.getId());
        } catch (WebApplicationException e) {
            LOG.errorf("Transaction for saga %s failed! Rolling back...", saga.getId());
            rollback(saga, rollbackActions, e.getMessage(), e.getResponse().getStatus());
            LOG.infof("Successful rollback for saga %s", saga.getId());
        }
    }

    @Override
    public SagaResponseDto createOrderTransactionAsync(UUID idempotencyKey, CreateOrderRequestDto createOrderRequestDto, UUID userId) {
        SagaEntity newSaga = start(idempotencyKey);
        managedExecutor.runAsync(() -> {
            try {
                LOG.infof("Started saga %s", newSaga.getId());
                createOrderTransaction(newSaga.getId(), idempotencyKey, createOrderRequestDto, userId);
            } catch (Exception e) {
                LOG.errorf(e, "Saga %s failed asynchronously", newSaga.getId());
            }
        });
        return sagaMapper.toDto(newSaga);
    }

    @Override
    public SagaResponseDto createOrderTransactionSync(UUID idempotencyKey, CreateOrderRequestDto createOrderRequestDto, UUID userId) {
        SagaEntity newSaga = start(idempotencyKey);
        try {
            LOG.infof("Started saga %s", newSaga.getId());
            createOrderTransaction(newSaga.getId(), idempotencyKey, createOrderRequestDto, userId);
        } catch (Exception e) {
            LOG.errorf(e, "Saga %s failed asynchronously", newSaga.getId());
        }
        return sagaMapper.toDto(newSaga);
    }

    @Override
    public SagaResponseDto getSaga(UUID id) {
        return sagaMapper.toDto(sagaRepository
                .findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Saga not found")));
    }

    @Override
    public SagaState getState(UUID id) {
        SagaEntity saga = sagaRepository
                .findByIdOptional(id)
                .orElseThrow(() -> {
                    LOG.errorf("Saga %s not found", id);
                    return new NotFoundException("Saga not found");
                });
        LOG.infof("Fetched status for saga %s", saga.getId());
        return saga.getState();
    }

    @Transactional
    public SagaEntity start(UUID idempotencyKey) {
        if (idempotencyKey == null) {
            LOG.errorf("Idempotency key omitted while attempting to start a saga");
            throw new BadRequestException("Idempotency key omitted");
        }

        Optional<SagaEntity> optionalSaga = sagaRepository
                .find("idempotencyKey", idempotencyKey)
                .firstResultOptional();
        if (optionalSaga.isPresent()) {
            return optionalSaga.get();
        }

        SagaEntity saga = new SagaEntity(
                null,
                SagaState.STARTED,
                Instant.now(),
                Instant.now(),
                "N/A",
                TransactionState.COMMITTING,
                new RecoveryDataEntity(),
                idempotencyKey);
        sagaRepository.persistAndFlush(saga);
        return saga;
    }

    private OrderResponseDto createOrder(SagaEntity saga, OrderRequestDto orderRequest) {
        OrderResponseDto orderResponseDto = orderClient.createOrder(orderRequest, tokenProvider.getAccessToken());
        saga.setState(SagaState.ORDER_CREATED);
        saga.setLastUpdated(Instant.now());
        saga.getRecoveryData().setOrderId(orderResponseDto.id());
        sagaRepository.persistAndFlush(saga);
        return orderResponseDto;
    }

    private InventoryResponseDto reserveInventory(SagaEntity saga, UUID productId, int amount) {
        InventoryResponseDto inventoryResponseDto = inventoryClient.reserve(productId, amount, tokenProvider.getAccessToken());
        saga.setState(SagaState.INVENTORY_RESERVED);
        saga.setLastUpdated(Instant.now());
        saga.getRecoveryData().setPrice(inventoryResponseDto.product().price());
        sagaRepository.persistAndFlush(saga);
        return inventoryResponseDto;
    }

    private PaymentResponseDto processPayment(SagaEntity saga, PaymentRequestDto paymentRequest) {
        PaymentResponseDto paymentResponseDto = paymentClient.process(paymentRequest, tokenProvider.getAccessToken());
        saga.setState(SagaState.PAYMENT_COMPLETED);
        saga.setLastUpdated(Instant.now());
        saga.getRecoveryData().setPaymentId(paymentResponseDto.id());
        sagaRepository.persistAndFlush(saga);
        return paymentResponseDto;
    }

    private void commit(SagaEntity saga) {
        saga.setState(SagaState.COMPLETED);
        saga.setLastUpdated(Instant.now());
        sagaRepository.persistAndFlush(saga);
    }

    private void rollback(SagaEntity saga, LinkedList<RollbackAction> rollbackActions, String errorMessage, int errorStatus) {
        saga.setTransactionState(TransactionState.ROLLING_BACK);
        saga.getRecoveryData().setErrorMessage(errorMessage);
        saga.getRecoveryData().setErrorStatus(errorStatus);
        sagaRepository.persistAndFlush(saga);

        Iterator<RollbackAction> iterator = rollbackActions.descendingIterator();
        while (iterator.hasNext()) {
            RollbackAction action = iterator.next();
            boolean success = executeRollbackWithRetries(action);
            if (!success) {
                saga.setState(SagaState.COMPENSATION_FAILED);
                saga.setLastUpdated(Instant.now());
                saga.setFailureReason("Failed while rolling back: %s".formatted(errorMessage));
                sagaRepository.persistAndFlush(saga);
                return;
            }
        }

        saga.setState(SagaState.FAILED);
        saga.setLastUpdated(Instant.now());
        saga.setFailureReason(errorMessage);
        sagaRepository.persistAndFlush(saga);
    }

    private boolean executeRollbackWithRetries(RollbackAction action) {
        int maxRetries = 5;
        long delay = 500;

        int i;
        for (i = 0; i < maxRetries; i++) {
            try {
                action.rollback();
                break;
            } catch (Exception e) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ex) {
                    throw new ServerErrorException(ex.getMessage(), 500);
                }
                delay *= 2;
            }
        }

        return i < maxRetries;
    }

    private LinkedList<RollbackAction> getRollbackActionsForRecovery(SagaEntity saga) {
        LinkedList<RollbackAction> rollbackActions = new LinkedList<>();

        SagaState sagaState = saga.getState();

        if (sagaState == SagaState.ORDER_CREATED || sagaState == SagaState.INVENTORY_RESERVED || sagaState == SagaState.PAYMENT_COMPLETED) {
            final UUID orderId = saga.getRecoveryData().getOrderId();
            rollbackActions.add(() -> orderClient.cancelOrder(orderId, tokenProvider.getAccessToken()));
        }
        if (sagaState == SagaState.INVENTORY_RESERVED || sagaState == SagaState.PAYMENT_COMPLETED) {
            final UUID productId = saga.getRecoveryData().getProductId();
            final int amount = saga.getRecoveryData().getAmount();
            rollbackActions.add(() -> inventoryClient.release(productId, amount, tokenProvider.getAccessToken()));
        }
        if (sagaState == SagaState.PAYMENT_COMPLETED) {
            final UUID paymentId = saga.getRecoveryData().getPaymentId();
            rollbackActions.add(() -> paymentClient.refund(paymentId, tokenProvider.getAccessToken()));
        }

        return rollbackActions;
    }

    private OrderResponseDto recoverOrder(SagaEntity saga, LinkedList<RollbackAction> rollbackActions) {
        OrderResponseDto orderResponseDto = createOrder(
                saga,
                new OrderRequestDto(
                        saga.getRecoveryData().getProductId(),
                        saga.getRecoveryData().getAmount(),
                        saga.getRecoveryData().getUserId()
                )
        );
        final UUID orderId = saga.getRecoveryData().getOrderId();
        rollbackActions.add(() -> orderClient.cancelOrder(orderId, tokenProvider.getAccessToken()));
        return orderResponseDto;
    }

    private InventoryResponseDto recoverInventory(SagaEntity saga, LinkedList<RollbackAction> rollbackActions) {
        InventoryResponseDto inventoryResponseDto = reserveInventory(
                saga,
                saga.getRecoveryData().getProductId(),
                saga.getRecoveryData().getAmount()
        );
        final UUID productId = saga.getRecoveryData().getProductId();
        final int amount = saga.getRecoveryData().getAmount();
        rollbackActions.add(() -> inventoryClient.release(productId, amount, tokenProvider.getAccessToken()));
        return inventoryResponseDto;
    }

    private PaymentResponseDto recoverPayment(SagaEntity saga, LinkedList<RollbackAction> rollbackActions, BigDecimal price) {
        PaymentResponseDto paymentResponseDto = processPayment(
                saga,
                new PaymentRequestDto(
                        price,
                        saga.getRecoveryData().getProductId(),
                        saga.getRecoveryData().getAmount(),
                        saga.getRecoveryData().getUserId()
                )
        );
        final UUID paymentId = saga.getRecoveryData().getPaymentId();
        rollbackActions.add(() -> paymentClient.refund(paymentId, tokenProvider.getAccessToken()));
        return paymentResponseDto;
    }

}
