package org.ftn.inventory.mapper.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ftn.inventory.dto.InventoryRequestDto;
import org.ftn.inventory.dto.InventoryResponseDto;
import org.ftn.inventory.entity.InventoryEntity;
import org.ftn.inventory.mapper.InventoryMapper;
import org.ftn.inventory.mapper.ProductMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@ApplicationScoped
public class InventoryMapperImpl implements InventoryMapper {
    private final ProductMapper productMapper;

    @Inject
    public InventoryMapperImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public InventoryEntity toEntity(InventoryRequestDto dto) {
        InventoryEntity entity = new InventoryEntity();
        entity.setAvailableStock(dto.availableStock());
        entity.setProduct(productMapper.toEntity(dto.product()));
        entity.setCreatedAt(Instant.now());
        entity.setLastUpdatedAt(Instant.now());
        return entity;
    }

    @Override
    public InventoryResponseDto toDto(InventoryEntity entity) {
        return new InventoryResponseDto(
                entity.getId(),
                productMapper.toDto(entity.getProduct()),
                entity.getAvailableStock(),
                LocalDateTime.ofInstant(entity.getCreatedAt(), ZoneId.systemDefault()),
                LocalDateTime.ofInstant(entity.getLastUpdatedAt(), ZoneId.systemDefault())
        );
    }
}
