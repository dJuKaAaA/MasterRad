package org.ftn.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ParticipantDataDto(UUID id,
                                 UUID paymentId,
                                 UUID orderId,
                                 UUID productId,
                                 int amount,
                                 BigDecimal price,
                                 UUID userId) {
}
