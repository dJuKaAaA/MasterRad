package org.ftn.dto;

import java.util.UUID;

public record KafkaInventoryRequestDto(UUID userId,
                                       UUID sagaId,
                                       UUID orderId,
                                       ProductReserveRequestDto productReserveRequestDto) {
}
