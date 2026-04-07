package org.ftn.resource;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.ftn.consts.Utils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.util.*;

public class WebShopServicesTestContainersManager implements QuarkusTestResourceLifecycleManager {
    private static final Set<PostgreSQLContainer<?>> dbContainers = new HashSet<>();
    private static final Set<GenericContainer<?>> serviceContainers = new HashSet<>();
    static final Network SHARED_NETWORK = Network.newNetwork();

    @Override
    public Map<String, String> start() {
        Map<String, String> config = new HashMap<>();
        for (String serviceName : List.of("order", "inventory", "payment")) {
            String networkAlias = "%s-tcc".formatted(serviceName);
            PostgreSQLContainer<?> dbContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:14"))
                    .withDatabaseName("tcc_%s".formatted(serviceName))
                    .withUsername(serviceName)
                    .withPassword(serviceName)
                    .withNetwork(SHARED_NETWORK)
                    .withNetworkAliases(networkAlias);
            dbContainer.start();

            String jdbcUrl = String.format("jdbc:postgresql://%s:5432/tcc_%s", networkAlias, serviceName);
            GenericContainer<?> serviceContainer = new GenericContainer<>(DockerImageName.parse("quarkus/%s-service-tcc-jvm:latest".formatted(serviceName))).withExposedPorts(8080)
                    .withEnv("quarkus.datasource.jdbc.url", jdbcUrl)
                    .withEnv("quarkus.datasource.username", dbContainer.getUsername())
                    .withEnv("quarkus.datasource.password", dbContainer.getPassword())
                    .withEnv("quarkus.hibernate-orm.database.generation", "drop-and-create")
                    .withEnv("quarkus.profile", "test")
                    .withEnv("QUARKUS_OIDC_TOKEN_ISSUER", Utils.getKeycloakUrl())
                    .withEnv("QUARKUS_OIDC_AUTH_SERVER_URL", Utils.getKeycloakUrl())
                    .withEnv("QUARKUS_OIDC_CLIENT_ID", "web-store")
                    .withNetwork(SHARED_NETWORK)
                    .dependsOn(dbContainer)
                    .waitingFor(Wait.forListeningPort());
            serviceContainer.start();

            String infoUrl = String.format("http://%s:%s", serviceContainer.getHost(), serviceContainer.getMappedPort(8080));
            config.put("quarkus.rest-client.%s-client.url".formatted(serviceName), infoUrl);
            dbContainers.add(dbContainer);
            serviceContainers.add(serviceContainer);

            // Seed the database
            try (var connection = java.sql.DriverManager.getConnection(
                    dbContainer.getJdbcUrl(),
                    dbContainer.getUsername(),
                    dbContainer.getPassword());
                 var stmt = connection.createStatement()) {

                String script = Utils.getDBTestValuesScript(serviceName);
                if (script != null) {
                    for (String sql : script.split(";")) {
                        String trimmed = sql.trim();
                        if (!trimmed.isEmpty()) {
                            stmt.execute(trimmed);
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to seed database for " + serviceName, e);
            }
        }

        return config;
    }

    @Override
    public void stop() {
        dbContainers.stream().filter(Objects::nonNull).forEach(GenericContainer::stop);
        serviceContainers.stream().filter(Objects::nonNull).forEach(GenericContainer::stop);
    }
}
