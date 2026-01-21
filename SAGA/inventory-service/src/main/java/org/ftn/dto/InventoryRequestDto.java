package org.ftn.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryRequestDto(
        @NotNull(message = "Available stock omitted")
        @Min(value = 0, message = "Available stock cannot be lesser than 0")
        Integer availableStock,
        @NotNull(message = "Product omitted")
        ProductRequestDto product
) {
}
