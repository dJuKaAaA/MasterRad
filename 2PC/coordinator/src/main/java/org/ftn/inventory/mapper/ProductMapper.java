package org.ftn.inventory.mapper;

import org.ftn.inventory.dto.ProductRequestDto;
import org.ftn.inventory.dto.ProductResponseDto;
import org.ftn.inventory.entity.ProductEntity;

public interface ProductMapper {
    ProductEntity toEntity(ProductRequestDto dto);
    ProductResponseDto toDto(ProductEntity entity);
}
