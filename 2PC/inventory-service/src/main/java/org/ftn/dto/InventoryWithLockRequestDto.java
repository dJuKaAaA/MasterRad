package org.ftn.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InventoryWithLockRequestDto(
        @Min(value = 0, message = "Available stock cannot be lesser than 0")
        int availableStock,
        @NotNull(message = "Product omitted")
        ProductRequestDto product,
        @NotNull(message = "Transaction lock id omitted")
        UUID txId
) {
}
