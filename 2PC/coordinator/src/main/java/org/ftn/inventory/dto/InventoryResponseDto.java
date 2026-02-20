package org.ftn.inventory.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryResponseDto(UUID id,
                                   ProductResponseDto product,
                                   int availableStock,
                                   LocalDateTime createdAt,
                                   LocalDateTime lastUpdatedAt) {

}
