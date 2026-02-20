package org.ftn.order.service.impl;

import io.quarkus.agroal.DataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.ftn.order.constant.OrderStatus;
import org.ftn.order.dto.OrderRequestDto;
import org.ftn.order.dto.OrderResponseDto;
import org.ftn.order.entity.OrderEntity;
import org.ftn.order.mapper.OrderMapper;
import org.ftn.order.repository.OrderRepository;
import org.ftn.order.service.OrderService;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    private static final Logger LOG = Logger.getLogger(OrderServiceImpl.class);

    @Inject
    public OrderServiceImpl(@DataSource("order") OrderRepository orderRepository,
                            OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
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
        if (optionalOrder.isPresent()) {
            LOG.infof("Cancelling order %s", id);
            OrderEntity order = optionalOrder.get();
            order.setStatus(OrderStatus.CANCELED);
            orderRepository.persist(order);
            LOG.infof("Successfully cancelled order %s", order.getId());
        }
    }
}
