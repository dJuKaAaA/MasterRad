package org.ftn.inventory.mapper.impl;

import jakarta.enterprise.context.ApplicationScoped;
import org.ftn.inventory.constant.ProductStatus;
import org.ftn.inventory.dto.ProductRequestDto;
import org.ftn.inventory.dto.ProductResponseDto;
import org.ftn.inventory.entity.ProductEntity;
import org.ftn.inventory.mapper.ProductMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@ApplicationScoped
public class ProductMapperImpl implements ProductMapper {
    @Override
    public ProductEntity toEntity(ProductRequestDto dto) {
        ProductEntity entity = new ProductEntity();
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setPrice(dto.price());
        entity.setAddedAt(Instant.now());
        entity.setStatus(ProductStatus.ACTIVE);
        entity.setMerchantId(dto.merchantId());
        return entity;
    }

    @Override
    public ProductResponseDto toDto(ProductEntity entity) {
        return new ProductResponseDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getPrice(),
                LocalDateTime.ofInstant(entity.getAddedAt(), ZoneId.systemDefault()),
                entity.getMerchantId()
        );
    }
}
