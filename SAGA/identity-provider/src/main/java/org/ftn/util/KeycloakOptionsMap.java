package org.ftn.util;

import io.smallrye.config.ConfigMapping;

import java.util.Optional;

@ConfigMapping(prefix = "my.keycloak")
public interface KeycloakOptionsMap {
    String clientId();
    Optional<String> clientSecret();
    String grantType();
    String adminUsername();
    String adminPassword();
}
