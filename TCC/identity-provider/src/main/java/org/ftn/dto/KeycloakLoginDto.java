package org.ftn.dto;

public record KeycloakLoginDto(String username,
                               String password,
                               String clientId,
                               String clientSecret,
                               String grantType) {
}
