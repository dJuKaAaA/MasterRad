package org.ftn.dto;

import org.ftn.constant.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderWithLockResponseDto(UUID id,
                                       UUID productId,
                                       int quantity,
                                       UUID userId,
                                       LocalDateTime createdAt,
                                       OrderStatus status,
                                       boolean locked,
                                       UUID lockId) {
}
