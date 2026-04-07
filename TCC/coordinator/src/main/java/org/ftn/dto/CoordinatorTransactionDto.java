package org.ftn.dto;

import org.ftn.constant.CoordinatorTransactionState;
import org.ftn.constant.Decision;

import java.time.Instant;
import java.util.UUID;

public record CoordinatorTransactionDto(UUID id,
                                        CoordinatorTransactionState state,
                                        Decision decision,
                                        Instant createdAt,
                                        String abortReason,
                                        boolean completed,
                                        ParticipantDataDto participantData) {
}
