package org.ftn.dto;

import java.util.UUID;

public record KafkaOrderRequestDto(UUID userId,
                                   UUID sagaId,
                                   OrderRequestDto orderRequestDto) {
}
