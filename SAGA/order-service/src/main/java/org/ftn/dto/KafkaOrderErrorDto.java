package org.ftn.dto;

import java.util.UUID;

public record KafkaOrderErrorDto(UUID orderId,
                                 UUID sagaId,
                                 KafkaErrorDto error) {
}
