package org.ftn.mapper.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ftn.dto.OrderRequestDto;
import org.ftn.dto.OrderWithLockRequestDto;
import org.ftn.dto.OrderWithLockResponseDto;
import org.ftn.entity.OrderEntity;
import org.ftn.mapper.OrderMapper;
import org.ftn.mapper.OrderWithLockMapper;

import java.time.LocalDateTime;
import java.time.ZoneId;

@ApplicationScoped
public class OrderWithLockMapperImpl implements OrderWithLockMapper {
    @Override
    public OrderEntity toEntity(OrderWithLockRequestDto dto) {
        OrderEntity entity = new OrderEntity();
        entity.setProductId(dto.productId());
        entity.setQuantity(dto.quantity());
        entity.setUserId(dto.userId());
        entity.setLockId(dto.txId());
        return entity;
    }

    @Override
    public OrderWithLockResponseDto toDto(OrderEntity entity) {
        return new OrderWithLockResponseDto(
                entity.getId(),
                entity.getProductId(),
                entity.getQuantity(),
                entity.getUserId(),
                LocalDateTime.ofInstant(entity.getCreatedAt(), ZoneId.systemDefault()),
                entity.getStatus(),
                entity.isLocked(),
                entity.getLockId()
        );
    }
}
