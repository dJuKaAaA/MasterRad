package org.ftn.client.dto;

public record TokenResponseDto(String token,
                               int expiresIn) {
}
