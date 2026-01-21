package org.ftn.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequestDto(
        @NotNull(message = "Price omitted")
        @Min(value = 1, message = "Price cannot be negative")
        BigDecimal price,
        @NotNull(message = "Product id omitted")
        UUID productId,
        @NotNull(message = "Product quantity omitted")
        @Min(value = 1, message = "Product quantity must be at least 1")
        Integer productQuantity,
        @NotNull(message = "User id omitted")
        UUID userId
) {
}
