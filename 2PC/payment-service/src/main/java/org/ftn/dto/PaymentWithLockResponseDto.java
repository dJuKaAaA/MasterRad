package org.ftn.dto;

import org.ftn.constant.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentWithLockResponseDto(UUID id,
                                         BigDecimal price,
                                         UUID productId,
                                         int productQuantity,
                                         LocalDateTime payedAt,
                                         PaymentStatus status,
                                         UUID userId,
                                         BigDecimal totalPrice,
                                         boolean locked,
                                         UUID lockId) {
}
