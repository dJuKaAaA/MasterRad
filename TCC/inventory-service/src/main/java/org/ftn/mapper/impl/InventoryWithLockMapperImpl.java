package org.ftn.mapper.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ftn.dto.InventoryRequestDto;
import org.ftn.dto.InventoryResponseDto;
import org.ftn.dto.InventoryWithLockRequestDto;
import org.ftn.dto.InventoryWithLockResponseDto;
import org.ftn.entity.InventoryEntity;
import org.ftn.mapper.InventoryMapper;
import org.ftn.mapper.InventoryWithLockMapper;
import org.ftn.mapper.ProductMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@ApplicationScoped
public class InventoryWithLockMapperImpl implements InventoryWithLockMapper {
    private final ProductMapper productMapper;

    @Inject
    public InventoryWithLockMapperImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public InventoryEntity toEntity(InventoryWithLockRequestDto dto) {
        InventoryEntity entity = new InventoryEntity();
        entity.setAvailableStock(dto.availableStock());
        entity.setProduct(productMapper.toEntity(dto.product()));
        entity.setCreatedAt(Instant.now());
        entity.setLastUpdatedAt(Instant.now());
        entity.setLockId(dto.txId());
        return entity;
    }

    @Override
    public InventoryWithLockResponseDto toDto(InventoryEntity entity) {
        return new InventoryWithLockResponseDto(
                entity.getId(),
                productMapper.toDto(entity.getProduct()),
                entity.getAvailableStock(),
                LocalDateTime.ofInstant(entity.getCreatedAt(), ZoneId.systemDefault()),
                LocalDateTime.ofInstant(entity.getLastUpdatedAt(), ZoneId.systemDefault()),
                entity.isLocked(),
                entity.getLockId()
        );
    }
}
