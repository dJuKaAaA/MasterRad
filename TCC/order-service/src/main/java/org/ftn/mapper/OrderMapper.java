package org.ftn.mapper;

import org.ftn.dto.OrderRequestDto;
import org.ftn.dto.OrderResponseDto;
import org.ftn.entity.OrderEntity;

public interface OrderMapper {
    OrderEntity toEntity(OrderRequestDto dto);
    OrderResponseDto toDto(OrderEntity entity);
}
