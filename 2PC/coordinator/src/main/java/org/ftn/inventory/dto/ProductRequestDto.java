package org.ftn.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequestDto(
        @Size(max = 200, message = "Max characters exceeded (200)")
        @NotBlank(message = "Name omitted")
        String name,
        @Size(max = 500, message = "Max characters exceeded (200)")
        @NotBlank(message = "Description omitted")
        String description,
        @NotNull(message = "Price omitted")
        @Min(value = 1, message = "Price must be at least 1")
        BigDecimal price,
        UUID merchantId
) {
}
