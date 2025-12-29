package org.ftn.utils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.ftn.client.KeycloakClient;
import org.ftn.client.dto.KeycloakAuthResponse;
import org.ftn.client.dto.KeycloakLoginDto;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

@ApplicationScoped
public class ServiceAccountTokenProvider {
    private String accessToken;
    private Instant expiresAt;
    private final KeycloakClient keycloakClient;
    private final KeycloakLoginDto sagaOrchestratorLoginDto;

    private final ReentrantLock refreshLock = new ReentrantLock();
    private static final Logger LOG = Logger.getLogger(ServiceAccountTokenProvider.class);

    @Inject
    public ServiceAccountTokenProvider(@RestClient KeycloakClient keycloakClient,
                                       SagaOrchestratorConfigMap sagaConfigMap) {
        this.keycloakClient = keycloakClient;
        this.sagaOrchestratorLoginDto = new KeycloakLoginDto(
                sagaConfigMap.username(),
                sagaConfigMap.password(),
                sagaConfigMap.clientId(),
                sagaConfigMap.clientSecret().orElse(""),
                sagaConfigMap.grantType()
        );
    }

    private boolean isTokenValid() {
        return accessToken != null
                && expiresAt != null
                && Instant.now().isBefore(expiresAt);
    }

    public String getAccessToken() {
        if (isTokenValid()) {
            return accessToken;
        }

        refreshLock.lock();
        try {
            // Double-check after acquiring lock
            if (isTokenValid()) {
                return accessToken;
            }
            refreshToken();
            return accessToken;
        } finally {
            refreshLock.unlock();
        }
    }

    private void refreshToken() {
        LOG.info("Refreshing service account access token");

        KeycloakAuthResponse response = keycloakClient.authenticate(
                sagaOrchestratorLoginDto.grantType(),
                sagaOrchestratorLoginDto.clientId(),
                sagaOrchestratorLoginDto.clientSecret(),
                sagaOrchestratorLoginDto.username(),
                sagaOrchestratorLoginDto.password()
        );

        String token = response.access_token();
        Number expiresIn = response.expires_in();

        // Subtract buffer to avoid edge expiry
        this.accessToken = token;
        this.expiresAt = Instant.now().plusSeconds(expiresIn.longValue() - 10);
    }
}
