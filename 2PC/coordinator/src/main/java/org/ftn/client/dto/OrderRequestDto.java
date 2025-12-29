package org.ftn.client.dto;

import java.util.UUID;

public record OrderRequestDto(UUID productId,
                              int amount,
                              UUID userId,
                              UUID txId) {
}
