package org.ftn.order.mapper;

import org.ftn.order.dto.OrderRequestDto;
import org.ftn.order.dto.OrderResponseDto;
import org.ftn.order.entity.OrderEntity;

public interface OrderMapper {
    OrderEntity toEntity(OrderRequestDto dto);
    OrderResponseDto toDto(OrderEntity entity);
}
