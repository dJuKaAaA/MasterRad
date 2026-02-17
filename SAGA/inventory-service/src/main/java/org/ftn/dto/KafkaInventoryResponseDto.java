package org.ftn.dto;

import java.util.UUID;

public record KafkaInventoryResponseDto(UUID userId,
                                        UUID sagaId,
                                        UUID orderId,
                                        Integer amount,
                                        InventoryResponseDto inventoryResponseDto,
                                        KafkaErrorDto error) {
}
