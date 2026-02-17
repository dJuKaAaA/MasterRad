package org.ftn.service.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.ftn.client.KeycloakClient;
import org.ftn.dto.KeycloakAuthResponseDto;
import org.ftn.dto.LoginRequestDto;
import org.ftn.dto.TokenResponseDto;
import org.ftn.service.AuthService;
import org.ftn.util.KeycloakOptionsMap;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AuthServiceImpl implements AuthService {
    private final KeycloakOptionsMap keycloakOptionsMap;
    private final KeycloakClient keycloakClient;

    private static final Logger LOG = Logger.getLogger(AuthServiceImpl.class);

    @Inject
    public AuthServiceImpl(KeycloakOptionsMap keycloakOptionsMap,
                           @RestClient KeycloakClient keycloakClient) {
        this.keycloakOptionsMap = keycloakOptionsMap;
        this.keycloakClient = keycloakClient;
    }

    @Override
    public TokenResponseDto login(LoginRequestDto body) {
        KeycloakAuthResponseDto response = keycloakClient.authenticate(
                keycloakOptionsMap.grantType(),
                keycloakOptionsMap.clientId(),
                keycloakOptionsMap.clientSecret().orElse(""),
                body.username(),
                body.password()
        );

        LOG.infof("%s login success", body.username());
        return new TokenResponseDto(response.access_token(), response.expires_in());
    }
}
