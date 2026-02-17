package org.ftn.dto;

import java.util.UUID;

public record KafkaOrderResponseDto(UUID userId,
                                    UUID sagaId,
                                    OrderResponseDto orderResponseDto,
                                    KafkaErrorDto error) {
}
