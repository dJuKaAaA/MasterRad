package org.ftn.mapper;

import org.ftn.dto.InventoryRequestDto;
import org.ftn.dto.InventoryResponseDto;
import org.ftn.dto.ProductReserveRequestDto;
import org.ftn.entity.InventoryEntity;

public interface InventoryMapper {
    InventoryEntity toEntity(InventoryRequestDto dto);
    InventoryResponseDto toDto(InventoryEntity entity);
}
