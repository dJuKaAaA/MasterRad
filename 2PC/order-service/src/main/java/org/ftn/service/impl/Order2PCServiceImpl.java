package org.ftn.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ServerErrorException;
import org.ftn.constant.OrderStatus;
import org.ftn.constant.Vote;
import org.ftn.dto.OrderWithLockRequestDto;
import org.ftn.dto.VoteResponse;
import org.ftn.entity.OrderEntity;
import org.ftn.mapper.OrderWithLockMapper;
import org.ftn.repository.OrderRepository;
import org.ftn.service.Order2PCService;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class Order2PCServiceImpl implements Order2PCService {
    private final OrderRepository orderRepository;
    private final OrderWithLockMapper orderWithLockMapper;

    private static final Logger LOG = Logger.getLogger(Order2PCServiceImpl.class);

    @Inject
    public Order2PCServiceImpl(OrderRepository orderRepository,
                               OrderWithLockMapper orderWithLockMapper) {
        this.orderRepository = orderRepository;
        this.orderWithLockMapper = orderWithLockMapper;
    }

    @Transactional
    @Override
    public VoteResponse prepare(OrderWithLockRequestDto dto) {
        LOG.info("Creating order");
        OrderEntity order = orderWithLockMapper.toEntity(dto);
        order.setCreatedAt(Instant.now());
        order.setUserId(dto.userId());
        order.setLocked(true);
        orderRepository.persist(order);
        LOG.infof("Order saved with id: %s", order.getId());
        return new VoteResponse(Vote.YES, orderWithLockMapper.toDto(order));
    }

    @Transactional
    @Override
    public void commit(UUID id, UUID lockId) {
        LOG.infof("Committing order %s", id);
        OrderEntity order = orderRepository
                .findByIdOptional(id)
                .orElseThrow(() -> {
                    LOG.errorf("Order %s not found", id);
                    return new ServerErrorException("Order not found while attempting commit", 500);
                });
        if (!order.tryUnlock(lockId))
            throw new ServerErrorException("Could not unlock lock", 409);

        order.setStatus(OrderStatus.COMPLETE);
        LOG.infof("Successful commit for order %s", order.getId());
        orderRepository.persist(order);
    }

    @Transactional
    @Override
    public void rollback(UUID id, UUID lockId) {
        Optional<OrderEntity> optionalOrder = orderRepository
                .findByIdOptional(id);
        if (optionalOrder.isPresent()) {
            LOG.infof("Rolling back order %s", id);
            OrderEntity order = optionalOrder.get();
            if (!order.tryUnlock(lockId)) {
                LOG.errorf("Order %s is currently locked", order.getId());
                throw new ServerErrorException("Order is currently locked", 409);
            }

            order.setStatus(OrderStatus.CANCELED);
            LOG.infof("Successful rollback for order %s", order.getId());
            orderRepository.persist(order);
        }
    }
}
