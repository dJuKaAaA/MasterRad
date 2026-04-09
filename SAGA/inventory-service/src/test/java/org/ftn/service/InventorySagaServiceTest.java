package org.ftn.service;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.ftn.dto.*;
import org.ftn.entity.InventoryEntity;
import org.ftn.repository.InventoryRepository;
import org.ftn.util.InventoryDataSeeder;
import org.ftn.util.KafkaTestResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(KafkaTestResource.class)
public class InventorySagaServiceTest {
    @Inject
    InventorySagaService inventorySagaService;
    @Inject
    InventoryRepository inventoryRepository;
    @Inject
    @Any
    InMemoryConnector connector;
    @Inject
    InventoryDataSeeder seeder;

    @BeforeEach
    public void setup() {
        seeder.seed();
    }

    @Test
    public void testReserve_Success() {
        UUID productId = UUID.fromString("d572df76-b527-4e31-8aa3-9aa954d17100");
        int quantity = 50;

        InventoryEntity before = inventoryRepository.find("product.id", productId).firstResult();
        int expectedQuantity = before.getAvailableStock();

        InventoryResponseDto inventoryResponseDto = inventorySagaService.reserve(productId, quantity);

        inventoryRepository.getEntityManager().clear();

        InventoryEntity after = inventoryRepository.findById(inventoryResponseDto.id());
        assertNotNull(after);

        assertEquals(expectedQuantity, after.getAvailableStock() + quantity);
        assertEquals(expectedQuantity, inventoryResponseDto.availableStock() + quantity);
    }

    @Test
    public void testReserve_FailProductIdOmitted() {
        int quantity = 1;
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            inventorySagaService.reserve(null, quantity);
        });

        assertEquals("Product id omitted", exception.getMessage());
    }

    @Test
    public void testReserve_FailInventoryNotFound() {
        int quantity = 1;
        UUID productId = UUID.fromString("e572df76-b527-4e31-8aa3-9aa954d17100");
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            inventorySagaService.reserve(productId, quantity);
        });

        assertEquals("Inventory not found", exception.getMessage());
    }

    @Test
    public void testReserve_FailNotEnoughProductsInStock() {
        int quantity = 1000;
        UUID productId = UUID.fromString("d572df76-b527-4e31-8aa3-9aa954d17100");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            inventorySagaService.reserve(productId, quantity);
        });

        assertEquals("Insufficient stocks", exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
            "0",
            "-1",
            "-1000",
            "-1000000"
    })
    public void testReserve_FailInvalidAmount(int quantity) {
        UUID productId = UUID.fromString("d572df76-b527-4e31-8aa3-9aa954d17100");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            inventorySagaService.reserve(productId, quantity);
        });

        assertEquals("Amount cannot be less than 1", exception.getMessage());
    }

    @Test
    public void testReserve_FailProductDiscontinued() {
        UUID productId = UUID.fromString("d572df76-b527-4e31-8aa3-9aa954d17105");
        int quantity = 1;
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            inventorySagaService.reserve(productId, quantity);
        });

        assertEquals("Product is discontinued", exception.getMessage());
    }

    @Test
    public void testRelease_Success() {
        UUID productId = UUID.fromString("d572df76-b527-4e31-8aa3-9aa954d17103");
        int quantity = 1;
        InventoryEntity inventory = inventoryRepository.find("product.id", productId).firstResult();
        int expectedQuantity = inventory.getAvailableStock();

        inventorySagaService.release(productId, quantity);

        inventoryRepository.getEntityManager().clear();

        InventoryEntity releasedInventory = inventoryRepository.find("product.id", productId).firstResult();
        assertNotNull(releasedInventory);

        assertEquals(expectedQuantity, releasedInventory.getAvailableStock() - 1);
    }

    @Test
    public void testReserveProductSagaFlow() {
        // 1. Prepare the Mock Incoming Message
        InMemorySource<KafkaInventoryRequestDto> commitSource = connector.source("inventory-service-commit");
        InMemorySink<KafkaInventoryResponseDto> responseSink = connector.sink("inventory-service-response");

        UUID productId = UUID.fromString("d572df76-b527-4e31-8aa3-9aa954d17101");
        int quantity = 1;
        InventoryEntity inventory = inventoryRepository.find("product.id", productId).firstResult();
        int expectedQuantity = inventory.getAvailableStock();

        UUID userId = UUID.randomUUID();
        KafkaInventoryRequestDto payload = new KafkaInventoryRequestDto(userId, UUID.randomUUID(), UUID.randomUUID(), new ProductReserveRequestDto(productId, quantity, userId));

        // 2. Send the message to the @Incoming channel
        commitSource.send(payload);

        // 3. Assertions: Database State
        await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> responseSink.received().size() == 1);

        // 4. Assertions: Outgoing Kafka Event
        assertEquals(1, responseSink.received().size(), "Should have sent 1 response message");

        inventoryRepository.getEntityManager().clear();

        InventoryEntity reservedInventory = inventoryRepository.find("product.id", productId).firstResult();

        KafkaInventoryResponseDto response = responseSink.received().getFirst().getPayload();
        assertEquals(payload.sagaId(), response.sagaId(), "Saga ID must match for correlation");
        assertEquals(payload.userId(), response.userId(), "User ID must match for correlation");
        assertNotNull(response.inventoryResponseDto(), "Inventory response should be populated");

        assertEquals(productId, response.inventoryResponseDto().product().id());
        assertEquals(expectedQuantity, response.inventoryResponseDto().availableStock() + quantity);
        assertEquals(productId, reservedInventory.getProduct().getId());
        assertEquals(expectedQuantity, reservedInventory.getAvailableStock() + quantity);

        responseSink.clear();
    }

    @Test
    public void testReleaseProductSagaFlow() {
        InMemorySource<KafkaInventoryErrorDto> rollbackSource = connector.source("inventory-service-rollback");
        InMemorySink<KafkaInventoryErrorDto> errorSink = connector.sink("inventory-service-error");

        int quantity = 1;
        UUID productId = UUID.fromString("d572df76-b527-4e31-8aa3-9aa954d17102");
        InventoryEntity inventory = inventoryRepository.find("product.id", productId).firstResult();
        int expectedQuantity = inventory.getAvailableStock();

        KafkaInventoryErrorDto errorPayload = new KafkaInventoryErrorDto(productId, UUID.randomUUID(), UUID.randomUUID(), quantity, new KafkaErrorDto("Product reservation failed", 400));

        rollbackSource.send(errorPayload);

        await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> errorSink.received().size() == 1);

        assertEquals(1, errorSink.received().size());

        inventoryRepository.getEntityManager().clear();

        InventoryEntity releasedInventory = inventoryRepository.find("product.id", productId).firstResult();
        assertNotNull(releasedInventory);

        assertEquals(expectedQuantity, releasedInventory.getAvailableStock() - 1);

        errorSink.clear();
    }
}
