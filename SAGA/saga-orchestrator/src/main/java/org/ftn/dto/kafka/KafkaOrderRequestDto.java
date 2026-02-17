package org.ftn.dto.kafka;

import org.ftn.client.dto.OrderRequestDto;

import java.util.UUID;

public record KafkaOrderRequestDto(UUID userId,
                                   UUID sagaId,
                                   OrderRequestDto orderRequestDto) {
}
