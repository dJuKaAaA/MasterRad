package org.ftn.service.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ServerErrorException;
import org.ftn.constant.OrderStatus;
import org.ftn.dto.OrderRequestDto;
import org.ftn.dto.OrderResponseDto;
import org.ftn.entity.OrderEntity;
import org.ftn.mapper.OrderMapper;
import org.ftn.repository.OrderRepository;
import org.ftn.service.OrderTCCService;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class OrderTCCServiceImpl implements OrderTCCService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    private static final Logger LOG = Logger.getLogger(OrderTCCServiceImpl.class);

    @Inject
    public OrderTCCServiceImpl(OrderRepository orderRepository, OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }

    @Transactional
    @Override
    public OrderResponseDto tccTry(OrderRequestDto dto) {
        LOG.info("Creating order");
        OrderEntity order = orderMapper.toEntity(dto);
        order.setCreatedAt(Instant.now());
        order.setUserId(dto.userId());
        order.setStatus(OrderStatus.PENDING);
        orderRepository.persist(order);
        LOG.infof("Order saved with id: %s", order.getId());
        return orderMapper.toDto(order);
    }

    @Transactional
    @Override
    public void tccCommit(UUID id) {
        LOG.infof("Committing order %s", id);
        OrderEntity order = orderRepository
                .findByIdOptional(id)
                .orElseThrow(() -> {
                    LOG.errorf("Order %s not found", id);
                    return new ServerErrorException("Order not found while attempting commit", 500);
                });

        order.setStatus(OrderStatus.COMPLETE);
        LOG.infof("Successful commit for order %s", order.getId());
        orderRepository.persist(order);

    }

    @Transactional
    @Override
    public void tccCancel(UUID id) {
        Optional<OrderEntity> optionalOrder = orderRepository
                .findByIdOptional(id);
        if (optionalOrder.isPresent()) {
            LOG.infof("Rolling back order %s", id);
            OrderEntity order = optionalOrder.get();
            order.setStatus(OrderStatus.CANCELED);
            LOG.infof("Successful rollback for order %s", order.getId());
            orderRepository.persist(order);
        }
    }
}
