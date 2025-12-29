package org.ftn.dto;

public record KeycloakAuthResponseDto(String access_token,
                                      int expires_in,
                                      int refresh_expires_in,
                                      String refresh_token) {
}
