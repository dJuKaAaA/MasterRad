package org.ftn.service.impl;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.ftn.constant.OrderStatus;
import org.ftn.dto.OrderResponseDto;
import org.ftn.dto.PageResponse;
import org.ftn.entity.OrderEntity;
import org.ftn.mapper.OrderMapper;
import org.ftn.repository.OrderRepository;
import org.ftn.service.OrderService;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    private static final Logger LOG = Logger.getLogger(OrderServiceImpl.class);

    @Inject
    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }

    @Override
    public PageResponse<OrderResponseDto> getAll(int page, int size) {
        PanacheQuery<OrderEntity> pageQuery = orderRepository
                .findAll()
                .page(page, size);
        int totalPages = pageQuery.pageCount();
        long totalSize = orderRepository.count();

        List<OrderResponseDto> ordersPage = pageQuery
                .list()
                .stream()
                .map(orderMapper::toDto)
                .toList();

        LOG.infof("Fetched %d/%d orders, page %d/%d, page size %d",
                ordersPage.size(),
                totalSize,
                page,
                totalPages,
                size
        );
        return new PageResponse<>(page, totalPages, ordersPage.size(), totalSize, ordersPage);
    }

    @Override
    public PageResponse<OrderResponseDto> getAll(int page, int size, OrderStatus status) {
        PanacheQuery<OrderEntity> pageQuery = orderRepository
                .find("status", status)
                .page(page, size);
        int totalPages = pageQuery.pageCount();
        long totalSize = orderRepository.count("status", status);

        List<OrderResponseDto> ordersPage = pageQuery
                .list()
                .stream()
                .map(orderMapper::toDto)
                .toList();

        LOG.infof("Fetched %d/%d orders for status %s, page %d/%d, page size %d",
                ordersPage.size(),
                totalSize,
                status,
                page,
                totalPages,
                size
        );
        return new PageResponse<>(page, totalPages, ordersPage.size(), totalSize, ordersPage);
    }

    @Override
    public OrderResponseDto get(UUID id) {
        return orderRepository
                .findByIdOptional(id)
                .map(orderMapper::toDto)
                .orElseThrow(() -> {
                    LOG.errorf("Order %s not found", id);
                    return new NotFoundException("Order not found");
                });
    }

    @Override
    public OrderResponseDto get(UUID id, UUID userId) {
        OrderEntity order = orderRepository
                .findByIdOptional(id)
                .orElseThrow(() -> {
                    LOG.errorf("Order %s not found", id);
                    return new NotFoundException("Order not found");
                });
        if (!order.getUserId().equals(userId)) {
            LOG.errorf("User id %s mismatch with %s while fetching order %s", userId, order.getUserId(), order.getId());
            throw new ForbiddenException("User id mismatch for order");
        }

        return orderMapper.toDto(order);
    }

    @Override
    public PageResponse<OrderResponseDto> getAllByUserId(UUID userId, int page, int size) {
        PanacheQuery<OrderEntity> pageQuery = orderRepository
                .find("userId", userId)
                .page(page, size);
        int totalPages = pageQuery.pageCount();
        long totalSize = orderRepository.count("userId", userId);

        List<OrderResponseDto> ordersPage = pageQuery
                .list()
                .stream()
                .map(orderMapper::toDto)
                .toList();

        LOG.infof("Fetched %d/%d orders for user %s, page %d/%d, page size %d",
                ordersPage.size(),
                totalSize,
                userId,
                page,
                totalPages,
                size
        );
        return new PageResponse<>(page, totalPages, ordersPage.size(), totalSize, ordersPage);
    }

    @Override
    public PageResponse<OrderResponseDto> getAllByProductId(UUID productId, int page, int size) {
        PanacheQuery<OrderEntity> pageQuery = orderRepository
                .find("productId", productId)
                .page(page, size);
        int totalPages = pageQuery.pageCount();
        long totalSize = orderRepository.count("productId", productId);

        List<OrderResponseDto> ordersPage = pageQuery
                .list()
                .stream()
                .map(orderMapper::toDto)
                .toList();

        LOG.infof("Fetched %d/%d orders for product %s, page %d/%d, page size %d",
                ordersPage.size(),
                totalSize,
                productId,
                page,
                totalPages,
                size
        );
        return new PageResponse<>(page, totalPages, ordersPage.size(), totalSize, ordersPage);
    }
}
