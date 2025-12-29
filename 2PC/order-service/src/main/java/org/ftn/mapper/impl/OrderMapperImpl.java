package org.ftn.mapper.impl;

import jakarta.enterprise.context.ApplicationScoped;
import org.ftn.dto.OrderRequestDto;
import org.ftn.dto.OrderResponseDto;
import org.ftn.entity.OrderEntity;
import org.ftn.mapper.OrderMapper;

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
