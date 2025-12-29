package org.ftn.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponseDto(UUID id,
                                 BigDecimal price,
                                 UUID productId,
                                 int productQuantity,
                                 LocalDateTime payedAt,
                                 PaymentStatus status,
                                 String userId,
                                 BigDecimal totalPrice) {
}
