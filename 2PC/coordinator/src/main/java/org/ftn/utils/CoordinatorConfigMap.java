package org.ftn.utils;

import io.smallrye.config.ConfigMapping;

import java.util.Optional;

@ConfigMapping(prefix = "coordinator")
public interface CoordinatorConfigMap {
    String username();
    String password();
    String clientId();
    Optional<String> clientSecret();
    String grantType();
}
