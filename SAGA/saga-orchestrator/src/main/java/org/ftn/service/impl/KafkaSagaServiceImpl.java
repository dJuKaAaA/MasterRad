package org.ftn.service.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.ftn.client.dto.*;
import org.ftn.constant.SagaState;
import org.ftn.constant.TransactionState;
import org.ftn.dto.CreateOrderRequestDto;
import org.ftn.dto.SagaResponseDto;
import org.ftn.dto.kafka.*;
import org.ftn.entity.RecoveryDataEntity;
import org.ftn.entity.SagaEntity;
import org.ftn.mapper.SagaMapper;
import org.ftn.repository.SagaRepository;
import org.ftn.service.KafkaSagaService;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class KafkaSagaServiceImpl implements KafkaSagaService {
    private final SagaRepository sagaRepository;
    private final SagaMapper sagaMapper;
    private final Emitter<KafkaOrderRequestDto> orderCommitEmitter;
    private final Emitter<KafkaInventoryRequestDto> inventoryCommitEmitter;
    private final Emitter<KafkaPaymentRequestDto> paymentCommitEmitter;
    private final Emitter<KafkaOrderErrorDto> orderRollbackEmitter;
    private final Emitter<KafkaInventoryErrorDto> inventoryRollbackEmitter;
    private final Emitter<KafkaPaymentErrorDto> paymentRollbackEmitter;

    private static final Logger LOG = Logger.getLogger(KafkaSagaServiceImpl.class);

    @Inject
    public KafkaSagaServiceImpl(SagaRepository sagaRepository,
                                SagaMapper sagaMapper,
                                @Channel("order-service-commit") Emitter<KafkaOrderRequestDto> orderCommitEmitter,
                                @Channel("inventory-service-commit") Emitter<KafkaInventoryRequestDto> inventoryCommitEmitter,
                                @Channel("payment-service-commit") Emitter<KafkaPaymentRequestDto> paymentCommitEmitter,
                                @Channel("order-service-rollback") Emitter<KafkaOrderErrorDto> orderRollbackEmitter,
                                @Channel("inventory-service-rollback") Emitter<KafkaInventoryErrorDto> inventoryRollbackEmitter,
                                @Channel("payment-service-rollback") Emitter<KafkaPaymentErrorDto> paymentRollbackEmitter) {
        this.sagaRepository = sagaRepository;
        this.sagaMapper = sagaMapper;
        this.orderCommitEmitter = orderCommitEmitter;
        this.inventoryCommitEmitter = inventoryCommitEmitter;
        this.paymentCommitEmitter = paymentCommitEmitter;
        this.orderRollbackEmitter = orderRollbackEmitter;
        this.inventoryRollbackEmitter = inventoryRollbackEmitter;
        this.paymentRollbackEmitter = paymentRollbackEmitter;
    }

    @Transactional
    @Override
    public SagaResponseDto createOrderTransactionKafka(UUID idempotencyKey, CreateOrderRequestDto createOrderRequestDto, UUID userId) {
        SagaEntity saga = start(idempotencyKey);
        LOG.infof("Starting saga %s", saga.getId());

        // Set recovery data
        saga.getRecoveryData().setProductId(createOrderRequestDto.productId());
        saga.getRecoveryData().setAmount(createOrderRequestDto.amount());
        saga.getRecoveryData().setUserId(userId);
        sagaRepository.persistAndFlush(saga);

        // Kafka async messaging
        KafkaOrderRequestDto payload = new KafkaOrderRequestDto(
                userId,
                saga.getId(),
                new OrderRequestDto(
                        createOrderRequestDto.productId(),
                        createOrderRequestDto.amount(),
                        userId
                )
        );
        orderCommitEmitter.send(Message.of(payload));

        return sagaMapper.toDto(saga);
    }

    @Transactional
    @Incoming("order-service-response")
    public CompletionStage<Void> orderServiceResponse(Message<KafkaOrderResponseDto> msg) {
        OrderResponseDto orderResponseDto = msg.getPayload().orderResponseDto();
        UUID sagaId = msg.getPayload().sagaId();
        SagaEntity saga = sagaRepository.findById(sagaId);
        saga.setState(SagaState.ORDER_CREATED);
        saga.setLastUpdated(Instant.now());
        saga.getRecoveryData().setOrderId(orderResponseDto.id());
        sagaRepository.persistAndFlush(saga);

        LOG.infof("Created order %s for saga %s", orderResponseDto.id(), saga.getId());

        KafkaInventoryRequestDto payload = new KafkaInventoryRequestDto(
                msg.getPayload().userId(),
                sagaId,
                orderResponseDto.id(),
                new ProductReserveRequestDto(
                        orderResponseDto.productId(),
                        orderResponseDto.quantity(),
                        orderResponseDto.userId()
                )
        );
        inventoryCommitEmitter.send(Message.of(payload));

        return msg.ack();
    }

    @Transactional
    @Incoming("inventory-service-response")
    public CompletionStage<Void> inventoryServiceResponse(Message<KafkaInventoryResponseDto> msg) {
        UUID sagaId = msg.getPayload().sagaId();
        if (msg.getPayload().error() == null) {
            InventoryResponseDto inventoryResponseDto = msg.getPayload().inventoryResponseDto();
            SagaEntity saga = sagaRepository.findById(sagaId);
            saga.setState(SagaState.INVENTORY_RESERVED);
            saga.setLastUpdated(Instant.now());
            saga.getRecoveryData().setPrice(inventoryResponseDto.product().price());
            sagaRepository.persistAndFlush(saga);

            LOG.infof("Reserved product %s for saga %s", inventoryResponseDto.product().id(), saga.getId());

            KafkaPaymentRequestDto payload = new KafkaPaymentRequestDto(
                    msg.getPayload().userId(),
                    sagaId,
                    msg.getPayload().orderId(),
                    inventoryResponseDto.id(),
                    msg.getPayload().amount(),
                    new PaymentRequestDto(
                            inventoryResponseDto.product().price(),
                            inventoryResponseDto.product().id(),
                            msg.getPayload().amount(),
                            msg.getPayload().userId()
                    )
            );
            paymentCommitEmitter.send(Message.of(payload));
        } else {
            KafkaErrorDto error = msg.getPayload().error();
            SagaEntity saga = sagaRepository.findById(sagaId);
            saga.setTransactionState(TransactionState.ROLLING_BACK);
            saga.getRecoveryData().setErrorMessage(error.errorMessage());
            saga.getRecoveryData().setErrorStatus(error.errorStatus());
            sagaRepository.persistAndFlush(saga);

            LOG.errorf("Transaction for saga %s failed! Rolling back...", saga.getId());

            KafkaOrderErrorDto payload = new KafkaOrderErrorDto(
                    msg.getPayload().orderId(),
                    sagaId,
                    error
            );
            orderRollbackEmitter.send(Message.of(payload));
        }

        return msg.ack();
    }

    @Transactional
    @Incoming("payment-service-response")
    public CompletionStage<Void> paymentServiceResponse(Message<KafkaPaymentResponseDto> msg) {
        UUID sagaId = msg.getPayload().sagaId();
        if (msg.getPayload().error() == null) {
            PaymentResponseDto paymentResponseDto = msg.getPayload().paymentResponseDto();
            SagaEntity saga = sagaRepository.findById(sagaId);
            saga.setState(SagaState.PAYMENT_COMPLETED);
            saga.setLastUpdated(Instant.now());
            saga.getRecoveryData().setPaymentId(paymentResponseDto.id());
            sagaRepository.persistAndFlush(saga);
            LOG.infof("Processed payment %s for saga %s", paymentResponseDto.id(), saga.getId());

            saga.setState(SagaState.COMPLETED);
            saga.setLastUpdated(Instant.now());
            sagaRepository.persistAndFlush(saga);
            LOG.infof("Successfully commited transaction for saga %s", saga.getId());
        } else {
            KafkaErrorDto error = msg.getPayload().error();
            SagaEntity saga = sagaRepository.findById(sagaId);
            saga.setTransactionState(TransactionState.ROLLING_BACK);
            saga.getRecoveryData().setErrorMessage(error.errorMessage());
            saga.getRecoveryData().setErrorStatus(error.errorStatus());
            sagaRepository.persistAndFlush(saga);

            LOG.errorf("Transaction for saga %s failed! Rolling back...", saga.getId());

            KafkaInventoryErrorDto payload = new KafkaInventoryErrorDto(
                    msg.getPayload().productId(),
                    msg.getPayload().orderId(),
                    sagaId,
                    msg.getPayload().amount(),
                    error
            );
            inventoryRollbackEmitter.send(Message.of(payload));
        }

        return msg.ack();
    }

    @Transactional
    @Incoming("order-service-error")
    public CompletionStage<Void> orderServiceError(Message<KafkaOrderErrorDto> msg) {
        UUID sagaId = msg.getPayload().sagaId();
        String errorMessage = msg.getPayload().error().errorMessage();
        SagaEntity saga = sagaRepository.findById(sagaId);
        saga.setState(SagaState.FAILED);
        saga.setLastUpdated(Instant.now());
        saga.setFailureReason(errorMessage);
        sagaRepository.persistAndFlush(saga);

        LOG.infof("Successful rollback for saga %s", saga.getId());

        return msg.ack();
    }

    @Transactional
    @Incoming("inventory-service-error")
    public CompletionStage<Void> inventoryServiceError(Message<KafkaInventoryErrorDto> msg) {
        KafkaOrderErrorDto payload = new KafkaOrderErrorDto(
                msg.getPayload().orderId(),
                msg.getPayload().sagaId(),
                msg.getPayload().error()
        );
        orderRollbackEmitter.send(Message.of(payload));
        return msg.ack();
    }

    @Transactional
    @Incoming("payment-service-error")
    public CompletionStage<Void> paymentServiceError(Message<KafkaPaymentErrorDto> msg) {
        KafkaInventoryErrorDto payload = new KafkaInventoryErrorDto(
                msg.getPayload().productId(),
                msg.getPayload().orderId(),
                msg.getPayload().sagaId(),
                msg.getPayload().amount(),
                msg.getPayload().error()
        );
        inventoryRollbackEmitter.send(Message.of(payload));
        return msg.ack();
    }

    private SagaEntity start(UUID idempotencyKey) {
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
}
