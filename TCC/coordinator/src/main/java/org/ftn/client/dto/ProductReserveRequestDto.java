package org.ftn.client.dto;

import java.util.UUID;

public record ProductReserveRequestDto(UUID productId,
                                       Integer quantity,
                                       UUID userId) {
}
