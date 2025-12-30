package org.ftn.dto;

import jakarta.validation.constraints.*;

public record UserCreateRequestDto(
        @Size(max = 250, message = "First name exceeds max num of characters (250)")
        @NotBlank(message = "First name omitted")
        String firstName,
        @Size(max = 250, message = "Last name exceeds max num of characters (250)")
        @NotBlank(message = "Last name omitted")
        String lastName,
        @Size(max = 250, message = "Username exceeds max num of characters (250)")
        @NotBlank(message = "Username omitted")
        String username,
        @Email(message = "Invalid email format")
        @Size(max = 250, message = "Username exceeds max num of characters (250)")
        @NotBlank(message = "Email omitted")
        String email,
        @NotBlank(message = "Password omitted")
        @Size(min = 8, message = "Password must contain at least 8 characters")
        String password
) {
}
