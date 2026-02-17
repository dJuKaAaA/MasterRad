package org.ftn.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ProductReserveRequestDto(
        @NotNull(message = "Product id omitted")
        UUID productId,
        @Min(value = 1, message = "Quantity can be at least 1")
        int quantity,
        @NotNull(message = "User id omitted")
        UUID userId
) {
}
