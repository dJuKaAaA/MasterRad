package org.ftn.resource;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.ftn.consts.Utils;
import org.springframework.data.domain.Page;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

public class IdentityProviderTestContainer implements QuarkusTestResourceLifecycleManager {
    private static GenericContainer<?> container;

    @Override
    public Map<String, String> start() {
        Map<String, String> config = new HashMap<>();
        container = new GenericContainer<>(DockerImageName.parse("quarkus/identity-provider-tcc-jvm:latest")).withExposedPorts(8080)
                .withEnv("quarkus.profile", "test")
                .withEnv("QUARKUS_OIDC_TOKEN_ISSUER", Utils.getKeycloakUrl())
                .withEnv("QUARKUS_OIDC_AUTH_SERVER_URL", Utils.getKeycloakUrl())
                .withEnv("QUARKUS_OIDC_CLIENT_ID", "web-store")
                .withEnv("QUARKUS_OIDC_CREDENTIALS_SECRET", "")
                .withEnv("MY_KEYCLOAK_CLIENT_ID", "web-store")
                .withEnv("MY_KEYCLOAK_CLIENT_SECRET", "")
                .withEnv("MY_KEYCLOAK_GRANT_TYPE", "password")
                .withEnv("QUARKUS_REST_CLIENT_KEYCLOAK_CLIENT_URL", Utils.getKeycloakUrl())
                .withNetwork(WebShopServicesTestContainersManager.SHARED_NETWORK)
                .waitingFor(Wait.forListeningPort());
        container.start();

        String infoUrl = String.format("http://%s:%s", container.getHost(), container.getMappedPort(8080));
        config.put("quarkus.rest-client.identity-provider-client.url", infoUrl);

        return config;
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }
}
