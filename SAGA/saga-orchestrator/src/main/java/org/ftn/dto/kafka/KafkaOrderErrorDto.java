package org.ftn.dto.kafka;

import java.util.UUID;

public record KafkaOrderErrorDto(UUID orderId,
                                 UUID sagaId,
                                 KafkaErrorDto error) {
}
