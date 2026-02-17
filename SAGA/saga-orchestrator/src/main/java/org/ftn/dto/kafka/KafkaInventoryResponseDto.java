package org.ftn.dto.kafka;

import org.ftn.client.dto.InventoryResponseDto;

import java.util.UUID;

public record KafkaInventoryResponseDto(UUID userId,
                                        UUID sagaId,
                                        UUID orderId,
                                        Integer amount,
                                        InventoryResponseDto inventoryResponseDto,
                                        KafkaErrorDto error) {
}
