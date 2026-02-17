package org.ftn.dto;

import java.util.UUID;

public record KafkaInventoryErrorDto(UUID productId,
                                     UUID orderId,
                                     UUID sagaId,
                                     Integer amount,
                                     KafkaErrorDto error) {
}
