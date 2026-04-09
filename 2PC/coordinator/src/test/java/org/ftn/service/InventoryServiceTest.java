package org.ftn.service;

import io.quarkus.agroal.DataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.ftn.inventory.dto.InventoryResponseDto;
import org.ftn.inventory.entity.InventoryEntity;
import org.ftn.inventory.repository.InventoryRepository;
import org.ftn.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class InventoryServiceTest {
    @Inject
    InventoryService inventorySagaService;
    @DataSource("inventory")
    @Inject
    InventoryRepository inventoryRepository;

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

}
