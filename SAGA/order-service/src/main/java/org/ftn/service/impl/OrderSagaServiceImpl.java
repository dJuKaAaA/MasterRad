package org.ftn.service.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.ftn.constant.OrderStatus;
import org.ftn.dto.*;
import org.ftn.entity.OrderEntity;
import org.ftn.mapper.OrderMapper;
import org.ftn.repository.OrderRepository;
import org.ftn.service.OrderSagaService;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class OrderSagaServiceImpl implements OrderSagaService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final Emitter<KafkaOrderResponseDto> responseEmitter;
    private final Emitter<KafkaOrderErrorDto> errorEmitter;

    private static final Logger LOG = Logger.getLogger(OrderSagaServiceImpl.class);

    @Inject
    public OrderSagaServiceImpl(OrderRepository orderRepository,
                                OrderMapper orderMapper,
                                @Channel("order-service-response") Emitter<KafkaOrderResponseDto> responseEmitter,
                                @Channel("order-service-error") Emitter<KafkaOrderErrorDto> errorEmitter) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.responseEmitter = responseEmitter;
        this.errorEmitter = errorEmitter;
    }

    @Transactional
    @Override
    public OrderResponseDto createOrder(OrderRequestDto dto) {
        LOG.info("Creating order");
        OrderEntity order = orderMapper.toEntity(dto);
        order.setCreatedAt(Instant.now());
        order.setUserId(dto.userId());
        order.setStatus(OrderStatus.COMPLETE);
        orderRepository.persist(order);
        LOG.infof("Successfully created order %s", order.getId());

        return orderMapper.toDto(order);
    }

    @Transactional
    @Override
    public void cancelOrder(UUID id) {
        Optional<OrderEntity> optionalOrder = orderRepository.findByIdOptional(id);
        if (optionalOrder.isPresent())  {
            LOG.infof("Cancelling order %s", id);
            OrderEntity order = optionalOrder.get();
            order.setStatus(OrderStatus.CANCELED);
            orderRepository.persist(order);
            LOG.infof("Successfully cancelled order %s", order.getId());
        }
    }

    @Transactional
    @Incoming("order-service-commit")
    public CompletionStage<Void> createOrder(Message<KafkaOrderRequestDto> msg) {
        OrderResponseDto orderResponseDto = createOrder(msg.getPayload().orderRequestDto());

        KafkaOrderResponseDto payload = new KafkaOrderResponseDto(
                msg.getPayload().userId(),
                msg.getPayload().sagaId(),
                orderResponseDto,
                null
        );
        responseEmitter.send(Message.of(payload));

        return msg.ack();
    }

    @Transactional
    @Incoming("order-service-rollback")
    public CompletionStage<Void> cancelOrder(Message<KafkaOrderErrorDto> msg) {
        UUID id = msg.getPayload().orderId();
        cancelOrder(id);
        errorEmitter.send(msg);
        return msg.ack();
    }
}
