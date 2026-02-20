package org.ftn.payment.dto;

import org.ftn.payment.constant.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponseDto(UUID id,
                                 BigDecimal price,
                                 UUID productId,
                                 int productQuantity,
                                 LocalDateTime payedAt,
                                 PaymentStatus status,
                                 UUID userId,
                                 BigDecimal totalPrice) {
}
