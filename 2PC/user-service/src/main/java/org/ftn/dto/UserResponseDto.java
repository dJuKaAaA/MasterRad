package org.ftn.dto;

public record UserResponseDto(String id,
                              String username,
                              String firstName,
                              String lastName,
                              String email,
                              Boolean emailVerified,
                              Boolean enabled,
                              Long createdTimestamp) {
}
