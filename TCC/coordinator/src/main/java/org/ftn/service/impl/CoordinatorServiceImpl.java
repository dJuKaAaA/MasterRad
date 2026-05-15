package org.ftn.service.impl;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.ftn.client.InventoryClient;
import org.ftn.client.OrderClient;
import org.ftn.client.PaymentClient;
import org.ftn.client.dto.*;
import org.ftn.constant.CoordinatorTransactionState;
import org.ftn.constant.Decision;
import org.ftn.dto.CoordinatorTransactionDto;
import org.ftn.dto.CreateOrderRequestDto;
import org.ftn.entity.CoordinatorTransactionEntity;
import org.ftn.mapper.CoordinatorTransactionMapper;
import org.ftn.repository.CoordinatorTransactionRepository;
import org.ftn.service.CoordinatorService;
import org.ftn.utils.ServiceAccountTokenProvider;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class CoordinatorServiceImpl implements CoordinatorService {
    private final CoordinatorTransactionRepository coordinatorTransactionRepository;
    private final CoordinatorTransactionMapper coordinatorTransactionMapper;
    private final OrderClient orderClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final ServiceAccountTokenProvider tokenProvider;
    private final ManagedExecutor executor;

    private static final Logger LOG = Logger.getLogger(CoordinatorServiceImpl.class);

    @Inject
    public CoordinatorServiceImpl(CoordinatorTransactionRepository coordinatorTransactionRepository,
                                  CoordinatorTransactionMapper coordinatorTransactionMapper,
                                  @RestClient OrderClient orderClient,
                                  @RestClient InventoryClient inventoryClient,
                                  @RestClient PaymentClient paymentClient,
                                  ServiceAccountTokenProvider tokenProvider,
                                  ManagedExecutor executor) {
        this.coordinatorTransactionRepository = coordinatorTransactionRepository;
        this.coordinatorTransactionMapper = coordinatorTransactionMapper;
        this.orderClient = orderClient;
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
        this.tokenProvider = tokenProvider;
        this.executor = executor;
    }

    @Transactional
    @Startup
    public void recoverPreparedTransactions() {
        Collection<CoordinatorTransactionEntity> incompleteTransactions = coordinatorTransactionRepository
                .find("state not in ?1", Set.of(CoordinatorTransactionState.COMMITTED, CoordinatorTransactionState.ABORTED))
                .list();
        if (incompleteTransactions.isEmpty()) return;

        incompleteTransactions
                .parallelStream()
                .forEach(this::compensateTransaction);
    }

    @Override
    public CoordinatorTransactionDto createTransaction(CreateOrderRequestDto requestBody, UUID userId) {
        CoordinatorTransactionDto tx = createTx(requestBody, userId);
        LOG.infof("Start transaction %s", tx.id());

        executor.runAsync(() -> {
            try {
                executeTransaction(tx.id());
            } catch (Exception e) {
                if (e instanceof WebApplicationException webApplicationException) {
                    setTxAbort(tx.id(), webApplicationException.getResponse().readEntity(String.class));
                } else {
                    setTxAbort(tx.id(), "Connection error or other error occurred");
                }
                LOG.errorf("Aborting transaction %s", tx.id());
                executeRollback(tx.id());
            }
        });

        return tx;
    }

    @Transactional
    @Override
    public CoordinatorTransactionDto getTransaction(UUID id) {
        return coordinatorTransactionRepository
                .findByIdOptional(id)
                .map(coordinatorTransactionMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Coordinator transaction entity not found"));
    }

    @Override
    public CoordinatorTransactionState getState(UUID id) {
        return coordinatorTransactionRepository
            .findByIdOptional(id)
            .orElseThrow(() -> new NotFoundException("Coordinator transaction entity not found"))
            .getState();
    }

    private void executeTransaction(UUID txId) {
        setTxState(txId, CoordinatorTransactionState.PREPARING);
        LOG.infof("Preparing transaction %s", txId);

        tryTransaction(txId);
        setTxStateAndDecision(txId, CoordinatorTransactionState.COMMITTING, Decision.COMMIT);

        try {
            LOG.infof("Commiting transaction %s", txId);
            commitTransaction(txId);
            LOG.infof("Commit actions completed for transaction %s", txId);
        } catch (Exception e) {
            CoordinatorTransactionEntity tx = getById(txId);
            if (e instanceof WebApplicationException webApplicationException) {
                setTxAbort(tx.getId(), webApplicationException.getResponse().readEntity(String.class));
            } else {
                setTxAbort(tx.getId(), "Connection error or other error occurred");
            }
            LOG.errorf("Aborting transaction %s", tx.getId());
            executeRollback(tx.getId());
            return;
        }

        setTxCompleted(txId, true);
        LOG.infof("Transaction %s successfully commited", txId);
    }

    // setter methods
    // --------------
    private CoordinatorTransactionDto createTx(CreateOrderRequestDto requestBody, UUID userId) {
        return QuarkusTransaction.requiringNew().call(() -> {
            CoordinatorTransactionEntity tx = new CoordinatorTransactionEntity(
                    null,
                    CoordinatorTransactionState.STARTED,
                    null,
                    Instant.now(),
                    false
            );

            tx.getParticipantData().setAmount(requestBody.amount());
            tx.getParticipantData().setProductId(requestBody.productId());
            tx.getParticipantData().setUserId(userId);
            tx.getParticipantData().setOrderId(UUID.randomUUID());
            tx.getParticipantData().setPaymentId(UUID.randomUUID());
            coordinatorTransactionRepository.persist(tx);

            return coordinatorTransactionMapper.toDto(tx);
        });
    }

    private void setTxState(UUID txId, CoordinatorTransactionState state) {
        QuarkusTransaction.requiringNew().run(() -> {
            CoordinatorTransactionEntity tx = coordinatorTransactionRepository.findById(txId);
            tx.setState(state);
        });
    }

    private void setTxStateAndDecision(UUID txId, CoordinatorTransactionState state, Decision decision) {
        QuarkusTransaction.requiringNew().run(() -> {
            CoordinatorTransactionEntity tx = coordinatorTransactionRepository.findById(txId);
            tx.setState(state);
            tx.setDecision(decision);
        });
    }

    private void setTxAbort(UUID txId, String reason) {
        QuarkusTransaction.requiringNew().run(() -> {
            CoordinatorTransactionEntity tx = coordinatorTransactionRepository.findById(txId);
            tx.setStateWhenFailed(tx.getState());
            tx.setState(CoordinatorTransactionState.ABORTING);
            tx.setDecision(Decision.ABORT);
            tx.setAbortReason(reason);
        });
    }

    private void setTxCompleted(UUID txId, boolean success) {
        QuarkusTransaction.requiringNew().run(() -> {
            CoordinatorTransactionEntity tx = coordinatorTransactionRepository.findById(txId);
            tx.setState(success ? CoordinatorTransactionState.COMMITTED : CoordinatorTransactionState.ABORTED);
            tx.setCompleted(true);
        });
    }

    private void setTxOrderData(UUID txId, UUID orderId) {
        QuarkusTransaction.requiringNew().run(() -> {
            CoordinatorTransactionEntity tx = coordinatorTransactionRepository.findById(txId);
            tx.setState(CoordinatorTransactionState.ORDER_SERVICE_TRY);
            tx.getParticipantData().setOrderId(orderId);
        });
    }

    private void setTxInventoryData(UUID txId, BigDecimal price) {
        QuarkusTransaction.requiringNew().run(() -> {
            CoordinatorTransactionEntity tx = coordinatorTransactionRepository.findById(txId);
            tx.setState(CoordinatorTransactionState.INVENTORY_SERVICE_TRY);
            tx.getParticipantData().setPrice(price);
        });
    }

    private void setTxPaymentData(UUID txId, UUID paymentId) {
        QuarkusTransaction.requiringNew().run(() -> {
            CoordinatorTransactionEntity tx = coordinatorTransactionRepository.findById(txId);
            tx.setState(CoordinatorTransactionState.PAYMENT_SERVICE_TRY);
            tx.getParticipantData().setPaymentId(paymentId);
        });
    }

    private void setAbortReason(UUID txId, String reason) {
        QuarkusTransaction.requiringNew().run(() -> {
            CoordinatorTransactionEntity tx = getById(txId);
            tx.setAbortReason(reason);
        });
    }
    // --------------

    private CoordinatorTransactionEntity getById(UUID id) {
        return QuarkusTransaction.requiringNew().call(() -> coordinatorTransactionRepository
                .findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Coordinator transaction entity not found")));
    }

    private void executeRollback(UUID txId) {
        LOG.infof("Rolling back transaction %s ...", txId);
        try {
            cancelTransaction(txId);
            setTxState(txId, CoordinatorTransactionState.ABORTED);
        } catch (Exception e) {
            CoordinatorTransactionEntity tx = getById(txId);
            switch (tx.getDecision()) {
                case COMMIT:
                    LOG.errorf("Failed to commit for transaction %s", tx.getId());
                    setTxState(txId, CoordinatorTransactionState.COMMIT_FAILURE);
                    break;
                case ABORT:
                    LOG.errorf("Failed to roll back for transaction %s", tx.getId());
                    setTxState(txId, CoordinatorTransactionState.ROLLBACK_FAILURE);
                    break;
            }
            setAbortReason(txId, e.getMessage());
            return;
        }
        LOG.infof("Rollback actions completed for transaction %s", txId);

        setTxCompleted(txId, false);

        LOG.infof("Transaction %s successful roll back", txId);
    }

    private void compensateTransaction(CoordinatorTransactionEntity tx) {
        if (tx.getState().ordinal() < CoordinatorTransactionState.COMMITTING.ordinal()) {
            tryTransaction(tx.getId());
        }

        switch (tx.getDecision()) {
            case COMMIT:
                if (tx.getState().ordinal() < CoordinatorTransactionState.COMMITTED.ordinal()) {
                    try {
                        commitTransaction(tx.getId());
                    } catch (Exception e) {
                        if (e instanceof WebApplicationException webApplicationException) {
                            setTxAbort(tx.getId(), webApplicationException.getResponse().readEntity(String.class));
                        } else {
                            setTxAbort(tx.getId(), "Connection error or other error occurred");
                        }
                        LOG.errorf("Aborting transaction %s", tx.getId());
                        executeRollback(tx.getId());
                    }
                }
                break;
            case ABORT:
                if (tx.getState().ordinal() >= CoordinatorTransactionState.ABORTING.ordinal()) {
                    executeRollback(tx.getId());
                }
                break;
        }

    }

    private void tryTransaction(UUID txId) {
        CoordinatorTransactionEntity tx = getById(txId);

        if (tx.getState().ordinal() < CoordinatorTransactionState.ORDER_SERVICE_TRY.ordinal()) {
            OrderResponseDto orderResponseDto = orderClient.tccTry(
                    new OrderRequestDto(
                            tx.getParticipantData().getProductId(),
                            tx.getParticipantData().getAmount(),
                            tx.getParticipantData().getUserId(),
                            tx.getParticipantData().getOrderId()
                    ),
                    tokenProvider.getAccessToken()
            );
            setTxOrderData(txId, orderResponseDto.id());
            LOG.infof("Order prepared for transaction %s", tx.getId());
            tx = getById(txId);
        }
        if (tx.getState().ordinal() < CoordinatorTransactionState.INVENTORY_SERVICE_TRY.ordinal()) {
            InventoryResponseDto inventoryResponseDto = inventoryClient.tccTry(
                    tx.getParticipantData().getProductId(),
                    tx.getParticipantData().getAmount(),
                    tokenProvider.getAccessToken()
            );
            setTxInventoryData(txId, inventoryResponseDto.product().price());
            LOG.infof("Product prepared for transaction %s", tx.getId());
            tx = getById(txId);
        }
        if (tx.getState().ordinal() < CoordinatorTransactionState.PAYMENT_SERVICE_TRY.ordinal()) {
            PaymentResponseDto paymentResponseDto = paymentClient.tccTry(
                    new PaymentRequestDto(
                            tx.getParticipantData().getPrice(),
                            tx.getParticipantData().getProductId(),
                            tx.getParticipantData().getAmount(),
                            tx.getParticipantData().getUserId(),
                            tx.getParticipantData().getPaymentId()
                    ),
                    tokenProvider.getAccessToken()
            );
            setTxPaymentData(txId, paymentResponseDto.id());
            LOG.infof("Payment prepared for transaction %s", tx.getId());
            tx = getById(txId);
        }
    }

    private void commitTransaction(UUID txId) {
        CoordinatorTransactionEntity tx = getById(txId);

        if (tx.getState().ordinal() < CoordinatorTransactionState.ORDER_SERVICE_COMMIT.ordinal()) {
            orderClient.tccCommit(tx.getParticipantData().getOrderId(), tokenProvider.getAccessToken());
            setTxState(txId, CoordinatorTransactionState.ORDER_SERVICE_COMMIT);
        }
        if (tx.getState().ordinal() < CoordinatorTransactionState.INVENTORY_SERVICE_COMMIT.ordinal()) {
            inventoryClient.tccCommit(tx.getParticipantData().getProductId(), tx.getParticipantData().getAmount(), tokenProvider.getAccessToken());
            setTxState(txId, CoordinatorTransactionState.INVENTORY_SERVICE_COMMIT);
        }
        if (tx.getState().ordinal() < CoordinatorTransactionState.PAYMENT_SERVICE_COMMIT.ordinal()) {
            paymentClient.tccCommit(tx.getParticipantData().getPaymentId(), tokenProvider.getAccessToken());
            setTxState(txId, CoordinatorTransactionState.PAYMENT_SERVICE_COMMIT);
        }
    }

    private void cancelTransaction(UUID txId) {
        CoordinatorTransactionEntity tx = getById(txId);

//        if (tx.getState().ordinal() < CoordinatorTransactionState.PAYMENT_SERVICE_CANCEL.ordinal() &&
//                tx.getStateWhenFailed().ordinal() >= CoordinatorTransactionState.PAYMENT_SERVICE_TRY.ordinal()) {
        if (tx.getParticipantData().getPaymentId() != null) {
            LOG.info("Starting payment cancel...");
            paymentClient.tccCancel(
                    tx.getParticipantData().getPaymentId(),
                    tokenProvider.getAccessToken()
            );
            setTxState(txId, CoordinatorTransactionState.PAYMENT_SERVICE_CANCEL);
            tx.setState(CoordinatorTransactionState.PAYMENT_SERVICE_CANCEL);
            LOG.info("Cancelled payment");
        }
        if (tx.getState().ordinal() < CoordinatorTransactionState.INVENTORY_SERVICE_CANCEL.ordinal() &&
                tx.getStateWhenFailed().ordinal() >= CoordinatorTransactionState.INVENTORY_SERVICE_TRY.ordinal()) {
            LOG.info("Starting inventory reservation cancel...");
            inventoryClient.tccCancel(
                    tx.getParticipantData().getProductId(),
                    tx.getParticipantData().getAmount(),
                    tokenProvider.getAccessToken()
            );
            setTxState(txId, CoordinatorTransactionState.INVENTORY_SERVICE_CANCEL);
            tx.setState(CoordinatorTransactionState.INVENTORY_SERVICE_CANCEL);
            LOG.info("Cancelled inventory reservation");
        }
//        if (tx.getState().ordinal() < CoordinatorTransactionState.ORDER_SERVICE_CANCEL.ordinal() &&
//                tx.getStateWhenFailed().ordinal() >= CoordinatorTransactionState.ORDER_SERVICE_TRY.ordinal()) {
        if (tx.getParticipantData().getOrderId() != null) {
            LOG.info("Starting order cancel...");
            orderClient.tccCancel(
                    tx.getParticipantData().getOrderId(),
                    tokenProvider.getAccessToken()
            );
            setTxState(txId, CoordinatorTransactionState.ORDER_SERVICE_CANCEL);
            LOG.info("Cancelled order");
        }
    }
}
