package org.ftn.payment.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record WalletRequestDto(
        @NotNull(message = "User id omitted")
        UUID userId
) {
}
