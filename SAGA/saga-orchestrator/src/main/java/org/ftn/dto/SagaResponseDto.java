package org.ftn.dto;

import java.time.LocalDateTime;

public record SagaResponseDto(String id,
                              String state,
                              LocalDateTime createdAt,
                              LocalDateTime lastUpdated,
                              String failureReason,
                              String transactionState,
                              String idempotencyKey) {
}
