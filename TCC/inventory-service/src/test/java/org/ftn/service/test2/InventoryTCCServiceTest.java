package org.ftn.service.test2;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import org.ftn.dto.InventoryResponseDto;
import org.ftn.entity.InventoryEntity;
import org.ftn.repository.InventoryRepository;
import org.ftn.service.InventoryTCCService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
public class InventoryTCCServiceTest {
    @Inject
    InventoryTCCService inventoryTCCService;
    @Inject
    InventoryRepository inventoryRepository;

    @Test
    public void testTCCTry_Success() {
        UUID productId = UUID.fromString("d572df76-b527-4e31-8aa3-9aa954d17100");
        int quantity = 50;

        InventoryEntity before = inventoryRepository.find("product.id", productId).firstResult();
        int expectedReservedQuantity = before.getReservedAmount();

        InventoryResponseDto inventoryResponseDto = inventoryTCCService.tccTry(productId, quantity);

        inventoryRepository.getEntityManager().clear();

        InventoryEntity after = inventoryRepository.findById(inventoryResponseDto.id());
        assertNotNull(after);

        assertEquals(expectedReservedQuantity, after.getReservedAmount() - quantity);
    }

    @Test
    public void testTCCTry_FailProductIdOmitted() {
        int quantity = 1;
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            inventoryTCCService.tccTry(null, quantity);
        });

        assertEquals("Product id omitted", exception.getMessage());
    }

    @Test
    public void testTCCTry_FailInventoryNotFound() {
        int quantity = 1;
        UUID productId = UUID.fromString("e572df76-b527-4e31-8aa3-9aa954d17100");
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            inventoryTCCService.tccTry(productId, quantity);
        });

        assertEquals("Product not found", exception.getMessage());
    }

    @Test
    public void testTCCTry_FailNotEnoughProductsInStock() {
        int quantity = 1000;
        UUID productId = UUID.fromString("d572df76-b527-4e31-8aa3-9aa954d17100");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            inventoryTCCService.tccTry(productId, quantity);
        });

        assertEquals("All of the products are reserved", exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
            "0",
            "-1",
            "-1000",
            "-1000000"
    })
    public void testTCCTry_FailInvalidAmount(int quantity) {
        UUID productId = UUID.fromString("d572df76-b527-4e31-8aa3-9aa954d17100");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            inventoryTCCService.tccTry(productId, quantity);
        });

        assertEquals("Amount cannot be less than 1", exception.getMessage());
    }

    @Test
    public void testTCCTry_FailProductDiscontinued() {
        UUID productId = UUID.fromString("d572df76-b527-4e31-8aa3-9aa954d17105");
        int quantity = 1;

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            inventoryTCCService.tccTry(productId, quantity);
        });

        assertEquals("Product is discontinued", exception.getMessage());
    }

    @Test
    public void testTCCCommit_Success() {
        UUID productId = UUID.fromString("d572df76-b527-4e31-8aa3-9aa954d17101");
        int quantity = 2;

        InventoryEntity before = inventoryRepository.find("product.id", productId).firstResult();
        int expectedReservedQuantity = before.getReservedAmount();
        int expectedQuantity = before.getAvailableStock();

        InventoryResponseDto inventoryResponseDto = inventoryTCCService.tccTry(productId, quantity);

        inventoryTCCService.tccCommit(productId, quantity);

        inventoryRepository.getEntityManager().clear();

        InventoryEntity after = inventoryRepository.findById(inventoryResponseDto.id());

        assertEquals(expectedQuantity, after.getAvailableStock() + quantity);
        assertEquals(expectedReservedQuantity, after.getReservedAmount());
    }

    @Test
    public void testTCCCommit_FailInventoryNotFound() {
        // Setup
        UUID productId = UUID.randomUUID();
        int quantity = 2;

        ServerErrorException exception = assertThrows(ServerErrorException.class, () -> inventoryTCCService.tccCommit(productId, quantity));

        assertEquals("Inventory not found while attempting commit", exception.getMessage());
        assertEquals(500, exception.getResponse().getStatus());
    }

    @Test
    public void testTCCCancel_Success() {
        UUID productId = UUID.fromString("d572df76-b527-4e31-8aa3-9aa954d17103");
        int quantity = 1;
        InventoryEntity before = inventoryRepository.find("product.id", productId).firstResult();
        int expectedReservedQuantity = before.getReservedAmount();

        inventoryTCCService.tccTry(productId, quantity);

        inventoryTCCService.tccCancel(productId, quantity);

        inventoryRepository.getEntityManager().clear();

        InventoryEntity after = inventoryRepository.find("product.id", productId).firstResult();
        assertNotNull(after);

        assertEquals(expectedReservedQuantity, after.getReservedAmount(), "Reserved amount mismatch: %d != %d".formatted(expectedReservedQuantity, after.getReservedAmount()));
    }
}
