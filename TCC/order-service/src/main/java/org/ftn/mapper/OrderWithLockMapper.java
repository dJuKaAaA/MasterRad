package org.ftn.mapper;

import org.ftn.dto.OrderRequestDto;
import org.ftn.dto.OrderWithLockRequestDto;
import org.ftn.dto.OrderWithLockResponseDto;
import org.ftn.entity.OrderEntity;

public interface OrderWithLockMapper {
    OrderEntity toEntity(OrderWithLockRequestDto dto);
    OrderWithLockResponseDto toDto(OrderEntity entity);
}
