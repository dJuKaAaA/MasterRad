package org.ftn.client.dto;

import java.util.UUID;

public record ProductReserveRequestDto(UUID productId,
                                       int quantity,
                                       UUID userId) {
}
