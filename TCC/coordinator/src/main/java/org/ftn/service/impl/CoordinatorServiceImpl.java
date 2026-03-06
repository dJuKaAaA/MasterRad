package org.ftn.service.impl;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.ftn.client.InventoryClient;
import org.ftn.client.OrderClient;
import org.ftn.client.PaymentClient;
import org.ftn.client.dto.*;
import org.ftn.constant.CoordinatorTransactionState;
import org.ftn.constant.Decision;
import org.ftn.dto.CreateOrderRequestDto;
import org.ftn.entity.CoordinatorTransactionEntity;
import org.ftn.repository.CoordinatorTransactionRepository;
import org.ftn.service.CoordinatorService;
import org.ftn.utils.ServiceAccountTokenProvider;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class CoordinatorServiceImpl implements CoordinatorService {
    private final CoordinatorTransactionRepository coordinatorTransactionRepository;
    private final OrderClient orderClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final ServiceAccountTokenProvider tokenProvider;

    private static final Logger LOG = Logger.getLogger(CoordinatorServiceImpl.class);

    @Inject
    public CoordinatorServiceImpl(CoordinatorTransactionRepository coordinatorTransactionRepository,
                                  @RestClient OrderClient orderClient,
                                  @RestClient InventoryClient inventoryClient,
                                  @RestClient PaymentClient paymentClient,
                                  ServiceAccountTokenProvider tokenProvider) {
        this.coordinatorTransactionRepository = coordinatorTransactionRepository;
        this.orderClient = orderClient;
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
        this.tokenProvider = tokenProvider;
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

    @Transactional
    @Override
    public void createTransaction(CreateOrderRequestDto requestBody, UUID userId) {
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
        coordinatorTransactionRepository.persistAndFlush(tx);
        LOG.infof("Start transaction %s", tx.getId());

        tx.setState(CoordinatorTransactionState.PREPARING);
        coordinatorTransactionRepository.persistAndFlush(tx);

        try {
            executeTransaction(tx);
        } catch (WebApplicationException e) {
            executeRollback(tx);
        } catch (Exception e) {
            LOG.errorf("Something went wrong: %s", e.getMessage());
        }
    }

    private void executeTransaction(CoordinatorTransactionEntity tx) {
        LOG.infof("Preparing transaction %s", tx.getId());

        tryTransaction(tx);

        tx.setState(CoordinatorTransactionState.COMMITTING);
        tx.setDecision(Decision.COMMIT);
        coordinatorTransactionRepository.persistAndFlush(tx);

        LOG.infof("Commiting transaction %s", tx.getId());
        commitTransaction(tx);
        LOG.infof("Commit actions completed for transaction %s", tx.getId());

        tx.setState(CoordinatorTransactionState.COMMITTED);
        tx.setCompleted(true);
        coordinatorTransactionRepository.persistAndFlush(tx);
        LOG.infof("Transaction %s successfully commited", tx.getId());
    }

    private void executeRollback(CoordinatorTransactionEntity tx) {
        tx.setState(CoordinatorTransactionState.ABORTING);
        tx.setDecision(Decision.ABORT);
        coordinatorTransactionRepository.persistAndFlush(tx);
        LOG.errorf("Aborting transaction %s", tx.getId());

        LOG.infof("Rolling back transaction %s ...", tx.getId());
        try {
            cancelTransaction(tx);
            tx.setState(CoordinatorTransactionState.ABORTED);
            coordinatorTransactionRepository.persistAndFlush(tx);
        } catch (Exception ignored) {
            switch (tx.getDecision()) {
                case COMMIT:
                    LOG.errorf("Failed to commit for transaction %s", tx.getId());
                    tx.setState(CoordinatorTransactionState.COMMIT_FAILURE);
                    coordinatorTransactionRepository.persistAndFlush(tx);
                    break;
                case ABORT:
                    LOG.errorf("Failed to roll back for transaction %s", tx.getId());
                    tx.setState(CoordinatorTransactionState.ROLLBACK_FAILURE);
                    coordinatorTransactionRepository.persistAndFlush(tx);
                    break;
            }
        }
        LOG.infof("Rollback actions completed for transaction %s", tx.getId());

        tx.setState(CoordinatorTransactionState.ABORTED);
        tx.setCompleted(true);
        coordinatorTransactionRepository.persistAndFlush(tx);

        LOG.infof("Transaction %s successful roll back", tx.getId());
    }

    private void compensateTransaction(CoordinatorTransactionEntity tx) {
        if (tx.getState().ordinal() < CoordinatorTransactionState.COMMITTING.ordinal()) {
            tryTransaction(tx);
        }

        switch (tx.getDecision()) {
            case COMMIT:
                if (tx.getState().ordinal() < CoordinatorTransactionState.COMMITTED.ordinal()) {
                    try {
                        executeTransaction(tx);
                    } catch (WebApplicationException e) {
                        executeRollback(tx);
                    } catch (Exception e) {
                        LOG.errorf("Something went wrong: %s", e.getMessage());
                    }
                }
                break;
            case ABORT:
                if (tx.getState().ordinal() < CoordinatorTransactionState.ABORTING.ordinal()) {
                    executeTransaction(tx);
                }
                break;
        }

    }

    private void tryTransaction(CoordinatorTransactionEntity tx) {
        if (tx.getState().ordinal() < CoordinatorTransactionState.ORDER_SERVICE_TRY.ordinal()) {
            OrderResponseDto orderResponseDto = orderClient.tccTry(
                    new OrderRequestDto(
                            tx.getParticipantData().getProductId(),
                            tx.getParticipantData().getAmount(),
                            tx.getParticipantData().getUserId()
                    ),
                    tokenProvider.getAccessToken()
            );
            tx.setState(CoordinatorTransactionState.ORDER_SERVICE_TRY);
            tx.getParticipantData().setOrderId(orderResponseDto.id());
            coordinatorTransactionRepository.persistAndFlush(tx);
            LOG.infof("Order prepared for transaction %s", tx.getId());
        }
        if (tx.getState().ordinal() < CoordinatorTransactionState.INVENTORY_SERVICE_TRY.ordinal()) {
            InventoryResponseDto inventoryResponseDto = inventoryClient.tccTry(
                    tx.getParticipantData().getProductId(),
                    tx.getParticipantData().getAmount(),
                    tokenProvider.getAccessToken()
            );
            tx.setState(CoordinatorTransactionState.INVENTORY_SERVICE_TRY);
            tx.getParticipantData().setProductId(inventoryResponseDto.product().id());
            tx.getParticipantData().setAmount(tx.getParticipantData().getAmount());
            coordinatorTransactionRepository.persistAndFlush(tx);
            LOG.infof("Product prepared for transaction %s", tx.getId());
        }
        if (tx.getState().ordinal() < CoordinatorTransactionState.PAYMENT_SERVICE_TRY.ordinal()) {
            PaymentResponseDto paymentResponseDto = paymentClient.tccTry(
                    new PaymentRequestDto(
                            tx.getParticipantData().getPrice(),
                            tx.getParticipantData().getProductId(),
                            tx.getParticipantData().getAmount(),
                            tx.getParticipantData().getUserId()
                    ),
                    tokenProvider.getAccessToken()
            );
            tx.setState(CoordinatorTransactionState.PAYMENT_SERVICE_TRY);
            tx.getParticipantData().setPaymentId(paymentResponseDto.id());
            coordinatorTransactionRepository.persistAndFlush(tx);
            LOG.infof("Payment prepared for transaction %s", tx.getId());
        }
    }

    private void commitTransaction(CoordinatorTransactionEntity tx) {
        if (tx.getState().ordinal() < CoordinatorTransactionState.ORDER_SERVICE_COMMIT.ordinal()) {
            orderClient.tccCommit(tx.getParticipantData().getOrderId(), tokenProvider.getAccessToken());
            tx.setState(CoordinatorTransactionState.ORDER_SERVICE_COMMIT);
            coordinatorTransactionRepository.persistAndFlush(tx);
        }
        if (tx.getState().ordinal() < CoordinatorTransactionState.INVENTORY_SERVICE_COMMIT.ordinal()) {
            inventoryClient.tccCommit(tx.getParticipantData().getProductId(), tx.getParticipantData().getAmount(), tokenProvider.getAccessToken());
            tx.setState(CoordinatorTransactionState.INVENTORY_SERVICE_COMMIT);
            coordinatorTransactionRepository.persistAndFlush(tx);
        }
        if (tx.getState().ordinal() < CoordinatorTransactionState.PAYMENT_SERVICE_COMMIT.ordinal()) {
            paymentClient.tccCommit(tx.getParticipantData().getPaymentId(), tokenProvider.getAccessToken());
            tx.setState(CoordinatorTransactionState.PAYMENT_SERVICE_COMMIT);
            coordinatorTransactionRepository.persistAndFlush(tx);
        }
    }

    private void cancelTransaction(CoordinatorTransactionEntity tx) {
        if (tx.getState().ordinal() < CoordinatorTransactionState.PAYMENT_SERVICE_CANCEL.ordinal()) {
            paymentClient.tccCancel(
                    tx.getParticipantData().getPaymentId(),
                    tokenProvider.getAccessToken()
            );
            tx.setState(CoordinatorTransactionState.PAYMENT_SERVICE_CANCEL);
            coordinatorTransactionRepository.persistAndFlush(tx);
        }
        if (tx.getState().ordinal() < CoordinatorTransactionState.INVENTORY_SERVICE_CANCEL.ordinal()) {
            inventoryClient.tccCancel(
                    tx.getParticipantData().getProductId(),
                    tx.getParticipantData().getAmount(),
                    tokenProvider.getAccessToken()
            );
            tx.setState(CoordinatorTransactionState.INVENTORY_SERVICE_CANCEL);
            coordinatorTransactionRepository.persistAndFlush(tx);
        }
        if (tx.getState().ordinal() < CoordinatorTransactionState.ORDER_SERVICE_CANCEL.ordinal()) {
            orderClient.tccCancel(
                    tx.getParticipantData().getOrderId(),
                    tokenProvider.getAccessToken()
            );
            tx.setState(CoordinatorTransactionState.ORDER_SERVICE_CANCEL);
            coordinatorTransactionRepository.persistAndFlush(tx);
        }
    }
}
