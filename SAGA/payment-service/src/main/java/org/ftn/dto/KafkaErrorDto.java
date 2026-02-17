package org.ftn.dto;

public record KafkaErrorDto(String errorMessage,
                            Integer errorStatus) {
}
