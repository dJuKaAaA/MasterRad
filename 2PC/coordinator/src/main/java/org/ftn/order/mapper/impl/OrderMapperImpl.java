package org.ftn.order.mapper.impl;

import jakarta.enterprise.context.ApplicationScoped;
import org.ftn.order.dto.OrderRequestDto;
import org.ftn.order.dto.OrderResponseDto;
import org.ftn.order.entity.OrderEntity;
import org.ftn.order.mapper.OrderMapper;

import java.time.LocalDateTime;
import java.time.ZoneId;

@ApplicationScoped
public class OrderMapperImpl implements OrderMapper {
    @Override
    public OrderEntity toEntity(OrderRequestDto dto) {
        OrderEntity entity = new OrderEntity();
        entity.setProductId(dto.productId());
        entity.setQuantity(dto.quantity());
        entity.setUserId(dto.userId());
        return entity;
    }

    @Override
    public OrderResponseDto toDto(OrderEntity entity) {
        return new OrderResponseDto(
                entity.getId(),
                entity.getProductId(),
                entity.getQuantity(),
                entity.getUserId(),
                LocalDateTime.ofInstant(entity.getCreatedAt(), ZoneId.systemDefault()),
                entity.getStatus()
        );
    }
}
