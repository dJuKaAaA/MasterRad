package org.ftn.dto.kafka;

import org.ftn.client.dto.OrderResponseDto;

import java.util.UUID;

public record KafkaOrderResponseDto(UUID userId,
                                    UUID sagaId,
                                    OrderResponseDto orderResponseDto,
                                    KafkaErrorDto error) {
}
