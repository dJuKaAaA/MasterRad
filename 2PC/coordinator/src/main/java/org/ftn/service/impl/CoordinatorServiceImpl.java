package org.ftn.service.impl;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.ftn.client.InventoryClient;
import org.ftn.client.KeycloakClient;
import org.ftn.client.OrderClient;
import org.ftn.client.PaymentClient;
import org.ftn.client.dto.*;
import org.ftn.constant.CoordinatorTransactionState;
import org.ftn.constant.Decision;
import org.ftn.dto.CreateOrderRequestDto;
import org.ftn.entity.CoordinatorTransactionEntity;
import org.ftn.entity.ParticipantDataEntity;
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
                                  @RestClient KeycloakClient keycloakClient,
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
                orderClient.commit(
                        tx.getParticipantData().getOrderId(),
                        tx.getId(),
                        tokenProvider.getAccessToken()
                );
                LOG.infof("Order service commited for transaction %s", tx.getId());

                inventoryClient.commit(
                        tx.getParticipantData().getProductId(),
                        tx.getParticipantData().getAmount(),
                        tx.getId(),
                        tokenProvider.getAccessToken()
                );
                LOG.infof("Inventory service commited for transaction %s", tx.getId());

                paymentClient.commit(
                        tx.getParticipantData().getPaymentId(),
                        tx.getId(),
                        tokenProvider.getAccessToken()
                );
                LOG.infof("Payment service commited for transaction %s", tx.getId());
                LOG.infof("Successful commits for transaction %s", tx.getId());
            } else {
                LOG.infof("Continuing with rollbacks for transaction %s", tx.getId());
                orderClient.rollback(
                        tx.getParticipantData().getOrderId(),
                        tx.getId(),
                        tokenProvider.getAccessToken()
                );
                LOG.infof("Order service rollback success for transaction %s", tx.getId());

                inventoryClient.rollback(
                        tx.getParticipantData().getProductId(),
                        tx.getParticipantData().getAmount(),
                        tx.getId(),
                        tokenProvider.getAccessToken()
                );
                LOG.infof("Inventory service rollback success for transaction %s", tx.getId());

                paymentClient.rollback(
                        tx.getParticipantData().getPaymentId(),
                        tx.getId(),
                        tokenProvider.getAccessToken()
                );
                LOG.infof("Payment service rollback success for transaction %s", tx.getId());
                LOG.infof("Successful rollbacks for transaction %s", tx.getId());
            }
        }
    }

    @Transactional
    @Override
    public void createTransaction(CreateOrderRequestDto createOrderRequestDto, UUID userId) {
        List<RollbackAction> rollbackActions = new LinkedList<>();
        List<CommitAction> commitActions = new LinkedList<>();

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

        LOG.infof("Prepare transaction %s", coordinatorTransactionEntity.getId());

        VoteResponseDto orderVoteResponse = prepareOrder(
                createOrderRequestDto,
                coordinatorTransactionEntity.getId(),
                userId,
                rollbackActions,
                commitActions
        );
        OrderResponseDto orderResponseDto = VoteResponseDto.getOrderResponse(orderVoteResponse);
        LOG.infof("Order %s prepared for transaction %s", orderResponseDto.id(), coordinatorTransactionEntity.getId());

        VoteResponseDto inventoryVoteResponse = prepareInventory(
                createOrderRequestDto,
                coordinatorTransactionEntity.getId(),
                rollbackActions,
                commitActions);
        InventoryResponseDto inventoryResponseDto = VoteResponseDto.getInventoryResponse(inventoryVoteResponse);
        LOG.infof("Product %s prepared for transaction %s", inventoryResponseDto.product().id(), coordinatorTransactionEntity.getId());

        VoteResponseDto paymentVoteResponse = preparePayment(
                createOrderRequestDto,
                coordinatorTransactionEntity.getId(),
                inventoryResponseDto.product().price(),
                userId,
                rollbackActions,
                commitActions);
        PaymentResponseDto paymentResponseDto = VoteResponseDto.getPaymentResponse(paymentVoteResponse);
        LOG.infof("Payment %s prepared for transaction %s", paymentResponseDto.id(), coordinatorTransactionEntity.getId());

        boolean readyToCommit = Stream.of(orderVoteResponse.vote(), inventoryVoteResponse.vote(), paymentVoteResponse.vote())
                .allMatch(vote -> vote == Vote.YES);

        // setting up data for recovery in case the coordinator crashes during committing or aborting
        ParticipantDataEntity participantData = new ParticipantDataEntity(
                null,
                paymentResponseDto.id(),
                orderResponseDto.id(),
                inventoryResponseDto.product().id(),
                createOrderRequestDto.amount()
        );
        coordinatorTransactionEntity.setParticipantData(participantData);
        if (readyToCommit) {
            coordinatorTransactionEntity.setState(CoordinatorTransactionState.COMMITTING);
            coordinatorTransactionEntity.setDecision(Decision.COMMIT);
            coordinatorTransactionRepository.persistAndFlush(coordinatorTransactionEntity);
            LOG.infof("Commiting transaction %s", coordinatorTransactionEntity.getId());

            int[] commitSuccesses = new int[commitActions.size()];
            IntStream
                    .range(0, commitSuccesses.length)
                    .parallel()
                    .forEach(i -> {
                        boolean success = executeCommitWithRetries(commitActions.get(i));
                        commitSuccesses[i] = success ? 1 : 0;
                    });
            if (Arrays.stream(commitSuccesses).anyMatch(success -> success == 0)) {
                LOG.errorf("Failed to roll back for transaction %s", coordinatorTransactionEntity.getId());
                coordinatorTransactionEntity.setState(CoordinatorTransactionState.COMMIT_FAILURE);
                coordinatorTransactionRepository.persistAndFlush(coordinatorTransactionEntity);
                return;
            }
            LOG.infof("Commit actions completed for transaction %s", coordinatorTransactionEntity.getId());

            coordinatorTransactionEntity.setState(CoordinatorTransactionState.COMMITTED);
            coordinatorTransactionEntity.setCompleted(true);
            coordinatorTransactionRepository.persistAndFlush(coordinatorTransactionEntity);
            LOG.infof("Transaction %s successfully commited", coordinatorTransactionEntity.getId());
        } else {
            coordinatorTransactionEntity.setState(CoordinatorTransactionState.ABORTING);
            coordinatorTransactionEntity.setDecision(Decision.ABORT);
            coordinatorTransactionRepository.persistAndFlush(coordinatorTransactionEntity);
            LOG.errorf("Aborting transaction %s", coordinatorTransactionEntity.getId());

            int[] rollbackSuccess = new int[rollbackActions.size()];
            IntStream
                    .range(0, rollbackSuccess.length)
                    .parallel()
                    .forEach(i -> {
                        boolean success = executeRollbackWithRetries(rollbackActions.get(i));
                        rollbackSuccess[i] = success ? 1 : 0;
                    });
            if (Arrays.stream(rollbackSuccess).anyMatch(success -> success == 0)) {
                LOG.errorf("Failed to roll back for transaction %s", coordinatorTransactionEntity.getId());
                coordinatorTransactionEntity.setState(CoordinatorTransactionState.ROLLBACK_FAILURE);
                coordinatorTransactionRepository.persistAndFlush(coordinatorTransactionEntity);
                return;
            }
            LOG.infof("Rollback actions completed for transaction %s", coordinatorTransactionEntity.getId());

            coordinatorTransactionEntity.setState(CoordinatorTransactionState.ABORTED);
            coordinatorTransactionEntity.setCompleted(true);
            coordinatorTransactionRepository.persistAndFlush(coordinatorTransactionEntity);

            LOG.infof("Transaction %s successful roll back", coordinatorTransactionEntity.getId());

            Stream.of(orderVoteResponse, inventoryVoteResponse, paymentVoteResponse)
                    .filter(e -> e.body() instanceof ErrorResponseDto)
                    .map(e -> (ErrorResponseDto) e.body())
                    .findAny()
                    .ifPresent(e -> {
                        throw new ClientErrorException(e.message(), e.status());
                    });
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

    private VoteResponseDto prepareOrder(CreateOrderRequestDto createOrderRequestDto,
                                         UUID txId,
                                         UUID userId,
                                         List<RollbackAction> rollbackActions,
                                         List<CommitAction> commitActions) {
        VoteResponseDto response ;
        try {
            response = orderClient.prepare(
                    new OrderRequestDto(
                            createOrderRequestDto.productId(),
                            createOrderRequestDto.amount(),
                            userId,
                            txId
                    ),
                    tokenProvider.getAccessToken()
            );
        } catch (WebApplicationException e) {
            LOG.errorf("Order client error: %s", e.getMessage());
            throw new ClientErrorException(e.getMessage(), e.getResponse().getStatus());
        }

        if (response.vote() == Vote.NO)
            return response;

        OrderResponseDto orderResponseDto = VoteResponseDto.getOrderResponse(response);
        if (orderResponseDto == null) {
            LOG.errorf("Could not convert VoteResponseDto to OrderResponseDto");
            throw new ServerErrorException("Internal server error", 500);
        }

        rollbackActions.add(() -> orderClient.rollback(
                orderResponseDto.id(),
                txId,
                tokenProvider.getAccessToken()
        ));
        commitActions.add(() -> orderClient.commit(
                orderResponseDto.id(),
                txId,
                tokenProvider.getAccessToken()
        ));

        return response;
    }

    private VoteResponseDto prepareInventory(CreateOrderRequestDto createOrderRequestDto,
                                             UUID txId,
                                             List<RollbackAction> rollbackActions,
                                             List<CommitAction> commitActions) {
        VoteResponseDto response;
        try {
            response = inventoryClient.prepare(
                    createOrderRequestDto.productId(),
                    createOrderRequestDto.amount(),
                    txId,
                    tokenProvider.getAccessToken()
            );
        } catch (WebApplicationException e) {
            LOG.errorf("Inventory client error: %s", e.getMessage());
            throw new ClientErrorException(e.getMessage(), e.getResponse().getStatus());
        }

        if (response.vote() == Vote.NO)
            return response;

        InventoryResponseDto inventoryResponseDto = VoteResponseDto.getInventoryResponse(response);
        if (inventoryResponseDto == null) {
            LOG.errorf("Could not convert VoteResponseDto to InventoryResponseDto");
            throw new ServerErrorException("Internal server error", 500);
        }

        rollbackActions.add(() -> inventoryClient.rollback(
                inventoryResponseDto.product().id(),
                createOrderRequestDto.amount(),
                txId,
                tokenProvider.getAccessToken()
        ));
        commitActions.add(() -> inventoryClient.commit(
                inventoryResponseDto.product().id(),
                createOrderRequestDto.amount(),
                txId,
                tokenProvider.getAccessToken()
        ));

        return response;
    }

    private VoteResponseDto preparePayment(CreateOrderRequestDto createOrderRequestDto,
                                           UUID txId,
                                           BigDecimal productPrice,
                                           UUID userId,
                                           List<RollbackAction> rollbackActions,
                                           List<CommitAction> commitActions) {

        VoteResponseDto response;
        try {
            response = paymentClient.prepare(
                    new PaymentRequestDto(
                            productPrice,
                            createOrderRequestDto.productId(),
                            createOrderRequestDto.amount(),
                            userId,
                            txId
                    ),
                    tokenProvider.getAccessToken()
            );
        } catch (WebApplicationException e) {
            LOG.errorf("Payment client error: %s", e.getMessage());
            throw new ServerErrorException(e.getMessage(), e.getResponse().getStatus());
        }

        if (response.vote() == Vote.NO)
            return response;

        PaymentResponseDto paymentResponseDto = VoteResponseDto.getPaymentResponse(response);
        if (paymentResponseDto == null) {
            LOG.errorf("Could not convert VoteResponseDto to PaymentResponseDto");
            throw new ServerErrorException("Internal server error", 500);
        }

        rollbackActions.add(() -> paymentClient.rollback(
                paymentResponseDto.id(),
                txId,
                tokenProvider.getAccessToken()
        ));
        commitActions.add(() -> paymentClient.commit(
                paymentResponseDto.id(),
                txId,
                tokenProvider.getAccessToken()
        ));

        return response;
    }
}
