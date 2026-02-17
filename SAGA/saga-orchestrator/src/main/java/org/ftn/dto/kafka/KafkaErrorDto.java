package org.ftn.dto.kafka;

public record KafkaErrorDto(String errorMessage,
                            Integer errorStatus) {
}
