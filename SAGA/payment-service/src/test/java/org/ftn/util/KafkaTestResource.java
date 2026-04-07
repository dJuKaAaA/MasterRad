package org.ftn.util;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;

import java.util.HashMap;
import java.util.Map;

public class KafkaTestResource implements QuarkusTestResourceLifecycleManager {
    @Override
    public Map<String, String> start() {
        Map<String, String> env = new HashMap<>();
        // Switch incoming channels to in-memory
        env.putAll(InMemoryConnector.switchIncomingChannelsToInMemory("payment-service-commit"));
        env.putAll(InMemoryConnector.switchIncomingChannelsToInMemory("payment-service-rollback"));

        // Switch outgoing channels to in-memory
        env.putAll(InMemoryConnector.switchOutgoingChannelsToInMemory("payment-service-response"));
        env.putAll(InMemoryConnector.switchOutgoingChannelsToInMemory("payment-service-error"));
        return env;
    }

    @Override
    public void stop() {
        InMemoryConnector.clear();
    }
}