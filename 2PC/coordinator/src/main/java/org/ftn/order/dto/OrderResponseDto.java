package org.ftn.order.dto;

import org.ftn.order.constant.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponseDto(UUID id,
                               UUID productId,
                               int quantity,
                               UUID userId,
                               LocalDateTime createdAt,
                               OrderStatus status) {
}
