package org.ftn.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryRequestDto(
        @Min(value = 0, message = "Available stock cannot be lesser than 0")
        int availableStock,
        @NotNull(message = "Product omitted")
        ProductRequestDto product
) {
}
