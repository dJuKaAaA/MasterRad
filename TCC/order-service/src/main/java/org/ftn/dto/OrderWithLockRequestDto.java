package org.ftn.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderWithLockRequestDto(
        @NotNull(message = "Product id omitted")
        UUID productId,
        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity,
        @NotNull(message = "User id omitted")
        UUID userId,
        @NotNull(message = "Transaction lock id omitted")
        UUID txId
) {
}
