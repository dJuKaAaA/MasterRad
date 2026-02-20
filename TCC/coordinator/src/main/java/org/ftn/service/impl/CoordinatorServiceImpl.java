package org.ftn.service.impl;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.ftn.client.InventoryClient;
import org.ftn.client.OrderClient;
import org.ftn.client.PaymentClient;
import org.ftn.client.dto.*;
import org.ftn.constant.CoordinatorTransactionState;
import org.ftn.constant.Decision;
import org.ftn.dto.CreateOrderRequestDto;
import org.ftn.entity.CoordinatorTransactionEntity;
import org.ftn.entity.ParticipantDataEntity;
import org.ftn.exception.PrepareException;
import org.ftn.repository.CoordinatorTransactionRepository;
import org.ftn.service.CoordinatorService;
import org.ftn.utils.CommitAction;
import org.ftn.utils.RollbackAction;
import org.ftn.utils.ServiceAccountTokenProvider;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
                .find("state in ?1", Set.of(CoordinatorTransactionState.COMMITTING, CoordinatorTransactionState.ABORTING))
                .list();
        if (incompleteTransactions.isEmpty()) return;

        for (CoordinatorTransactionEntity tx : incompleteTransactions) {
            if (tx.getState() == CoordinatorTransactionState.COMMITTING) {
                LOG.infof("Continuing with commits for transaction %s", tx.getId());
                orderClient.tccCommit(
                        tx.getParticipantData().getOrderId(),
                        tokenProvider.getAccessToken()
                );
                LOG.infof("Order service commited for transaction %s", tx.getId());

                inventoryClient.tccCommit(
                        tx.getParticipantData().getProductId(),
                        tx.getParticipantData().getAmount(),
                        tokenProvider.getAccessToken()
                );
                LOG.infof("Inventory service commited for transaction %s", tx.getId());

                paymentClient.tccCommit(
                        tx.getParticipantData().getPaymentId(),
                        tokenProvider.getAccessToken()
                );
                LOG.infof("Payment service commited for transaction %s", tx.getId());
                LOG.infof("Successful commits for transaction %s", tx.getId());
            } else {
                LOG.infof("Continuing with rollbacks for transaction %s", tx.getId());
                orderClient.tccCancel(
                        tx.getParticipantData().getOrderId(),
                        tokenProvider.getAccessToken()
                );
                LOG.infof("Order service rollback success for transaction %s", tx.getId());

                inventoryClient.tccCancel(
                        tx.getParticipantData().getProductId(),
                        tx.getParticipantData().getAmount(),
                        tokenProvider.getAccessToken()
                );
                LOG.infof("Inventory service rollback success for transaction %s", tx.getId());

                paymentClient.tccCancel(
                        tx.getParticipantData().getPaymentId(),
                        tokenProvider.getAccessToken()
                );
                LOG.infof("Payment service rollback success for transaction %s", tx.getId());
                LOG.infof("Successful rollbacks for transaction %s", tx.getId());
            }
        }
    }

    @Transactional
    @Override
    public void createTransaction(CreateOrderRequestDto requestBody, UUID userId) {
        List<RollbackAction> rollbackActions = new LinkedList<>();

        CoordinatorTransactionEntity coordinatorTransactionEntity = new CoordinatorTransactionEntity(
                null,
                CoordinatorTransactionState.STARTED,
                null,
                Instant.now(),
                false
        );
        coordinatorTransactionRepository.persistAndFlush(coordinatorTransactionEntity);
        LOG.infof("Start transaction %s", coordinatorTransactionEntity.getId());

        coordinatorTransactionEntity.setState(CoordinatorTransactionState.PREPARING);
        coordinatorTransactionRepository.persistAndFlush(coordinatorTransactionEntity);

        try {
            LOG.infof("Prepare transaction %s", coordinatorTransactionEntity.getId());

            OrderResponseDto orderResponseDto = orderClient.tccTry(
                    new OrderRequestDto(
                            requestBody.productId(),
                            requestBody.amount(),
                            userId
                    ),
                    tokenProvider.getAccessToken()
            );
            LOG.infof("Order prepared for transaction %s", coordinatorTransactionEntity.getId());
            rollbackActions.add(() -> orderClient.tccCancel(orderResponseDto.id(), tokenProvider.getAccessToken()));

            InventoryResponseDto inventoryResponseDto = inventoryClient.tccTry(
                    requestBody.productId(),
                    requestBody.amount(),
                    tokenProvider.getAccessToken()
            );
            LOG.infof("Product prepared for transaction %s", coordinatorTransactionEntity.getId());
            rollbackActions.add(() -> inventoryClient.tccCancel(requestBody.productId(), requestBody.amount(), tokenProvider.getAccessToken()));

            PaymentResponseDto paymentResponseDto = paymentClient.tccTry(
                    new PaymentRequestDto(
                            inventoryResponseDto.product().price(),
                            requestBody.productId(),
                            requestBody.amount(),
                            userId
                    ),
                    tokenProvider.getAccessToken()
            );
            LOG.infof("Payment prepared for transaction %s", coordinatorTransactionEntity.getId());
            rollbackActions.add(() -> paymentClient.tccCancel(paymentResponseDto.id(), tokenProvider.getAccessToken()));

            // setting up data for recovery in case the coordinator crashes during committing or aborting
            ParticipantDataEntity participantData = new ParticipantDataEntity(
                    null,
                    paymentResponseDto.id(),
                    orderResponseDto.id(),
                    inventoryResponseDto.product().id(),
                    requestBody.amount()
            );
            coordinatorTransactionEntity.setParticipantData(participantData);
            coordinatorTransactionEntity.setState(CoordinatorTransactionState.COMMITTING);
            coordinatorTransactionEntity.setDecision(Decision.COMMIT);
            coordinatorTransactionRepository.persistAndFlush(coordinatorTransactionEntity);

            LOG.infof("Commiting transaction %s", coordinatorTransactionEntity.getId());
            orderClient.tccCommit(orderResponseDto.id(), tokenProvider.getAccessToken());
            inventoryClient.tccCommit(requestBody.productId(), requestBody.amount(), tokenProvider.getAccessToken());
            orderClient.tccCommit(paymentResponseDto.id(), tokenProvider.getAccessToken());

            LOG.infof("Commit actions completed for transaction %s", coordinatorTransactionEntity.getId());

            coordinatorTransactionEntity.setState(CoordinatorTransactionState.COMMITTED);
            coordinatorTransactionEntity.setCompleted(true);
            coordinatorTransactionRepository.persistAndFlush(coordinatorTransactionEntity);
            LOG.infof("Transaction %s successfully commited", coordinatorTransactionEntity.getId());
        } catch (WebApplicationException e) {
            coordinatorTransactionEntity.setState(CoordinatorTransactionState.ABORTING);
            coordinatorTransactionEntity.setDecision(Decision.ABORT);
            coordinatorTransactionRepository.persistAndFlush(coordinatorTransactionEntity);
            LOG.errorf("Aborting transaction %s", coordinatorTransactionEntity.getId());

            LOG.infof("Rolling back transaction %s ...", coordinatorTransactionEntity.getId());
            try {
                rollbackActions.forEach(RollbackAction::rollback);
            } catch (Exception ignored) {
                switch (coordinatorTransactionEntity.getState()) {
                    case COMMITTING:
                        LOG.errorf("Failed to commit for transaction %s", coordinatorTransactionEntity.getId());
                        coordinatorTransactionEntity.setState(CoordinatorTransactionState.COMMIT_FAILURE);
                        coordinatorTransactionRepository.persistAndFlush(coordinatorTransactionEntity);
                        break;
                    case ABORTING:
                        LOG.errorf("Failed to roll back for transaction %s", coordinatorTransactionEntity.getId());
                        coordinatorTransactionEntity.setState(CoordinatorTransactionState.ROLLBACK_FAILURE);
                        coordinatorTransactionRepository.persistAndFlush(coordinatorTransactionEntity);
                        break;
                }
            }
            LOG.infof("Rollback actions completed for transaction %s", coordinatorTransactionEntity.getId());

            coordinatorTransactionEntity.setState(CoordinatorTransactionState.ABORTED);
            coordinatorTransactionEntity.setCompleted(true);
            coordinatorTransactionRepository.persistAndFlush(coordinatorTransactionEntity);

            LOG.infof("Transaction %s successful roll back", coordinatorTransactionEntity.getId());
        } catch (Exception e) {
            LOG.errorf("Something went wrong: %s", e.getMessage());
        }
    }

    private boolean executeCommitWithRetries(CommitAction action) {
        int maxRetries = 5;
        long delay = 500;

        int i;
        for (i = 0; i < maxRetries; i++) {
            try {
                action.commit();
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
}
