package org.ftn.dto;


import jakarta.validation.constraints.NotNull;

public record LoginRequestDto(
        @NotNull(message = "Username omitted")
        String username,
        @NotNull(message = " omitted")
        String password
) {
}
