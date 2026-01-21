package org.ftn.client.dto;

import java.util.UUID;

public record OrderRequestDto(UUID productId,
                              Integer quantity,
                              UUID userId) {
}
