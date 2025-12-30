package org.ftn.util;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

@ApplicationScoped
public class KeycloakProvider {
    private final KeycloakOptionsMap keycloakOptionsMap;

    @Inject
    public KeycloakProvider(KeycloakOptionsMap keycloakOptionsMap) {
        this.keycloakOptionsMap = keycloakOptionsMap;
    }

    @Produces
    @ApplicationScoped
    public Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(keycloakOptionsMap.serverUrl())
                .realm(keycloakOptionsMap.realm())
                .grantType(keycloakOptionsMap.grantType())
                .clientId(keycloakOptionsMap.clientId())
                .clientSecret(keycloakOptionsMap.clientSecret().orElse(""))
                .username(keycloakOptionsMap.adminUsername())
                .password(keycloakOptionsMap.adminPassword())
                .build();
    }

}
