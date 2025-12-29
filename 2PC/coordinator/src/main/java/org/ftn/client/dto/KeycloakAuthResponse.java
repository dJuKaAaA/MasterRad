package org.ftn.client.dto;

public record KeycloakAuthResponse(String access_token,
                                   int expires_in,
                                   int refresh_expires_in,
                                   String refresh_token) {
}
