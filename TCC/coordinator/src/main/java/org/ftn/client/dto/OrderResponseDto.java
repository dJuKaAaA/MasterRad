package org.ftn.client.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponseDto(UUID id,
                               UUID productId,
                               int quantity,
                               UUID userId,
                               LocalDateTime createdAt,
                               OrderStatus status) {
}
