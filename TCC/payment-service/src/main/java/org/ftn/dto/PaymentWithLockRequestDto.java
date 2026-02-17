package org.ftn.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentWithLockRequestDto(
        @Min(value = 0, message = "Price cannot be negative")
        BigDecimal price,
        @NotNull(message = "Product id omitted")
        UUID productId,
        @Min(value = 1, message = "Product quantity must be at least 1")
        int productQuantity,
        @NotNull(message = "User id omitted")
        UUID userId,
        @NotNull(message = "Transaction lock id omitted")
        UUID txId
) {
}
