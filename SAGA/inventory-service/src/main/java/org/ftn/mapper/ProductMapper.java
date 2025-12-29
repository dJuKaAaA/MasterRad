package org.ftn.mapper;

import org.ftn.dto.ProductRequestDto;
import org.ftn.dto.ProductResponseDto;
import org.ftn.entity.ProductEntity;

public interface ProductMapper {
    ProductEntity toEntity(ProductRequestDto dto);
    ProductResponseDto toDto(ProductEntity entity);
}
