package org.ftn.resource;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

public class KafkaTestContainerManager implements QuarkusTestResourceLifecycleManager {

    static final Network SHARED_NETWORK = Network.newNetwork(); // shared network

    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withNetwork(SHARED_NETWORK)
            .withNetworkAliases("kafka");

    static {
        kafka.start();
    }

    @Override
    public Map<String, String> start() {
        kafka.start();

        Map<String, String> config = new HashMap<>();
        config.put("kafka.bootstrap.servers", kafka.getBootstrapServers());
        config.put("mp.messaging.connector.smallrye-kafka.bootstrap.servers", kafka.getBootstrapServers());
        config.put("mp.messaging.outgoing.order-service-commit.topic", "order-service-commit");
        config.put("mp.messaging.outgoing.order-service-rollback.topic", "order-service-rollback");
        config.put("mp.messaging.outgoing.inventory-service-commit.topic", "inventory-service-commit");
        config.put("mp.messaging.outgoing.inventory-service-rollback.topic", "inventory-service-rollback");
        config.put("mp.messaging.outgoing.payment-service-commit.topic", "payment-service-commit");
        config.put("mp.messaging.outgoing.payment-service-rollback.topic", "payment-service-rollback");
        config.put("mp.messaging.incoming.order-service-response.topic", "order-service-response");
        config.put("mp.messaging.incoming.order-service-error.topic", "order-service-error");
        config.put("mp.messaging.incoming.inventory-service-response.topic", "inventory-service-response");
        config.put("mp.messaging.incoming.inventory-service-error.topic", "inventory-service-error");
        config.put("mp.messaging.incoming.payment-service-response.topic", "payment-service-response");
        config.put("mp.messaging.incoming.payment-service-error.topic", "payment-service-error");

        return config;
    }

    @Override
    public void stop() {
        if (kafka != null) {
            kafka.stop();
        }
    }
}