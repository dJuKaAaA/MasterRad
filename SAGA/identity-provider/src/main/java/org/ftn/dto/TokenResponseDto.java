package org.ftn.dto;

public record TokenResponseDto(String token,
                               int expiresIn) {
}
