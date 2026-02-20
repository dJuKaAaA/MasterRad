package org.ftn.inventory.mapper;

import org.ftn.inventory.dto.InventoryRequestDto;
import org.ftn.inventory.dto.InventoryResponseDto;
import org.ftn.inventory.entity.InventoryEntity;

public interface InventoryMapper {
    InventoryEntity toEntity(InventoryRequestDto dto);
    InventoryResponseDto toDto(InventoryEntity entity);
}
