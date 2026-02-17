package org.ftn.dto.kafka;

import org.ftn.client.dto.ProductReserveRequestDto;

import java.util.UUID;

public record KafkaInventoryRequestDto(UUID userId,
                                       UUID sagaId,
                                       UUID orderId,
                                       ProductReserveRequestDto productReserveRequestDto) {
}
