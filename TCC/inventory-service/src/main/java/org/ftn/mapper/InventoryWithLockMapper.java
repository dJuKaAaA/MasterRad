package org.ftn.mapper;

import org.ftn.dto.InventoryRequestDto;
import org.ftn.dto.InventoryResponseDto;
import org.ftn.dto.InventoryWithLockRequestDto;
import org.ftn.dto.InventoryWithLockResponseDto;
import org.ftn.entity.InventoryEntity;

public interface InventoryWithLockMapper {
    InventoryEntity toEntity(InventoryWithLockRequestDto dto);
    InventoryWithLockResponseDto toDto(InventoryEntity entity);
}
