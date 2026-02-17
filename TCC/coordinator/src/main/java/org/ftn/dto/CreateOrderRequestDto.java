package org.ftn.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateOrderRequestDto(
        @NotNull(message = "Product id omitted")
        UUID productId,
        @Min(value = 1, message = "Amount must be at least 1")
        int amount
) {
}
