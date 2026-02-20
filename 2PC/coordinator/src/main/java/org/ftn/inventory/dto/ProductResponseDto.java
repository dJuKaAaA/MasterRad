package org.ftn.inventory.dto;

import org.ftn.inventory.constant.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponseDto(UUID id,
                                 String name,
                                 String description,
                                 ProductStatus status,
                                 BigDecimal price,
                                 LocalDateTime addedAt,
                                 UUID merchantId) {
}
