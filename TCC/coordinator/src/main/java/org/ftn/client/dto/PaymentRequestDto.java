package org.ftn.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequestDto(BigDecimal price,
                                UUID productId,
                                Integer productQuantity,
                                UUID userId) {
}
