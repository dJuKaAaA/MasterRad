package org.ftn.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record OrderRequestDto(
        @NotNull(message = "Product id omitted")
        UUID productId,
        @Size(min = 1, message = "Quantity must be at least 1")
        int quantity,
        @NotNull(message = "User id omitted")
        UUID userId
) {
}
