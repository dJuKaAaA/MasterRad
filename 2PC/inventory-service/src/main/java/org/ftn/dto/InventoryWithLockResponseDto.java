package org.ftn.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryWithLockResponseDto(UUID id,
                                           ProductResponseDto product,
                                           int availableStock,
                                           LocalDateTime createdAt,
                                           LocalDateTime lastUpdatedAt,
                                           boolean locked,
                                           UUID lockId) {
}
