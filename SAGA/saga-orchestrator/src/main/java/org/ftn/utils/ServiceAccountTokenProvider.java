package org.ftn.utils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.ftn.client.IdentityProviderClient;
import org.ftn.client.dto.LoginRequestDto;
import org.ftn.client.dto.TokenResponseDto;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

@ApplicationScoped
public class ServiceAccountTokenProvider {
    private String accessToken;
    private Instant expiresAt;
    private final IdentityProviderClient identityProviderClient;
    private final SagaOrchestratorConfigMap sagaConfigMap;

    private final ReentrantLock refreshLock = new ReentrantLock();
    private static final Logger LOG = Logger.getLogger(ServiceAccountTokenProvider.class);

    @Inject
    public ServiceAccountTokenProvider(@RestClient IdentityProviderClient identityProviderClient,
                                       SagaOrchestratorConfigMap sagaConfigMap) {
        this.identityProviderClient = identityProviderClient;
        this.sagaConfigMap = sagaConfigMap;
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

        TokenResponseDto response = identityProviderClient.login(
                new LoginRequestDto(
                        sagaConfigMap.username(),
                        sagaConfigMap.password()
                )
        );

        String token = response.token();
        Number expiresIn = response.expiresIn();

        // Subtract buffer to avoid edge expiry
        this.accessToken = token;
        this.expiresAt = Instant.now().plusSeconds(expiresIn.longValue() - 10);
    }
}
