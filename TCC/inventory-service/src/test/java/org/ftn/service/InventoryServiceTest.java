package org.ftn.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.ws.rs.*;
import org.ftn.constant.ProductStatus;
import org.ftn.dto.*;
import org.ftn.entity.InventoryEntity;
import org.ftn.entity.ProductEntity;
import org.ftn.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class InventoryServiceTest {
    @Inject
    InventoryService inventoryService;
    @Inject
    InventoryRepository inventoryRepository;

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    public InventoryEntity getDummyInventory() {
        ProductEntity product = new ProductEntity();
        product.setName("New product name");
        product.setDescription("New product description");
        product.setPrice(BigDecimal.ONE);
        product.setAddedAt(Instant.now());
        product.setStatus(ProductStatus.ACTIVE);
        product.setMerchantId(UUID.randomUUID());
        InventoryEntity inventory = new InventoryEntity();
        inventory.setProduct(product);
        inventory.setAvailableStock(100);
        inventory.setCreatedAt(Instant.now());
        inventory.setLastUpdatedAt(Instant.now());
        return inventory;
    }

    @ParameterizedTest
    @CsvSource({
            "0, 5, 0, 5, 1, 5",
            "0, 10, 0, 5, 1, 5",
            "1, 2, 1, 2, 3, 5",
            "1, 3, 1, 2, 2, 5",
            "2, 5, 2, 0, 1, 5"
    })
    public void testGetAll(int page,
                           int size,
                           int expectedPage,
                           int expectedSize,
                           int expectedTotalPages,
                           long expectedTotalSize) {
        PageResponse<InventoryResponseDto> results = inventoryService.getAll(page, size);
        assertEquals(expectedPage, results.page(), "Expected page mismatch");
        assertEquals(expectedSize, results.size(), "Expected size mismatch");
        assertEquals(expectedTotalPages, results.totalPages(), "Expected total pages mismatch");
        assertEquals(expectedTotalSize, results.totalSize(), "Expected total size mismatch");
    }

    @ParameterizedTest
    @CsvSource({
            "76347922-6f4f-41df-8ff5-dae6bb66b69a, 0, 5, 0, 5, 1, 5",
            "76347922-6f4f-41df-8ff5-dae6bb66b69a, 0, 10, 0, 5, 1, 5",
            "76347922-6f4f-41df-8ff5-dae6bb66b69a, 1, 2, 1, 2, 3, 5",
            "76347922-6f4f-41df-8ff5-dae6bb66b69a, 1, 3, 1, 2, 2, 5",
            "76347922-6f4f-41df-8ff5-dae6bb66b69a, 2, 5, 2, 0, 1, 5",
            "76347922-6f4f-41df-8ff5-dae6bb66b69b, 0, 5, 0, 0, 1, 0"
    })
    public void testGetAllByMerchantId(UUID merchantId,
                                       int page,
                                       int size,
                                       int expectedPage,
                                       int expectedSize,
                                       int expectedTotalPages,
                                       long expectedTotalSize) {
        PageResponse<InventoryResponseDto> results = inventoryService.getAll(merchantId, page, size);
        assertEquals(expectedPage, results.page(), "Expected page mismatch");
        assertEquals(expectedSize, results.size(), "Expected size mismatch");
        assertEquals(expectedTotalPages, results.totalPages(), "Expected total pages mismatch");
        assertEquals(expectedTotalSize, results.totalSize(), "Expected total size mismatch");
    }

    @Test
    public void testGetById_Success() {
        UUID id = UUID.fromString("a3c1d2f5-4e6b-4b3f-9a2c-1e7d3f6b8c13");
        InventoryResponseDto inventory = inventoryService.get(id);
        assertEquals(id, inventory.id());
    }

    @Test
    public void testGetById_NotFound() {
        UUID id = UUID.fromString("b3c1d2f5-4e6b-4b3f-9a2c-1e7d3f6b8c13");
        NotFoundException exception = assertThrows(NotFoundException.class, () -> inventoryService.get(id));
        assertEquals("Inventory not found", exception.getMessage());
    }

    @Test
    public void testGetByIdAndMerchantId_Success() {
        UUID id = UUID.fromString("a3c1d2f5-4e6b-4b3f-9a2c-1e7d3f6b8c13");
        UUID merchantId = UUID.fromString("76347922-6f4f-41df-8ff5-dae6bb66b69a");
        InventoryResponseDto inventory = inventoryService.get(id, merchantId);
        assertEquals(id, inventory.id());
        assertEquals(merchantId, inventory.product().merchantId());
    }

    @Test
    public void testGetByIdAndMerchantId_NotFound() {
        UUID id = UUID.fromString("b3c1d2f5-4e6b-4b3f-9a2c-1e7d3f6b8c13");
        UUID merchantId = UUID.fromString("055c925e-b2fc-49af-bcfc-07913e52c82f");
        NotFoundException exception = assertThrows(NotFoundException.class, () -> inventoryService.get(id, merchantId));
        assertEquals("Inventory not found", exception.getMessage());
    }

    @Test
    public void testGetByIdAndMerchantId_MerchantMismatch() {
        UUID id = UUID.fromString("a3c1d2f5-4e6b-4b3f-9a2c-1e7d3f6b8c13");
        UUID merchantId = UUID.fromString("7e865ca7-a38e-4000-0000-fa6d01e9bdbf");
        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> inventoryService.get(id, merchantId));
        assertEquals("Merchant id doesn't match for inventory", exception.getMessage());
    }

    @Test
    public void testCreateInventory_Success() {
        // expected values
        InventoryEntity expectedInventory = getDummyInventory();
        ProductEntity expectedProduct = expectedInventory.getProduct();
        expectedProduct.setPrice(BigDecimal.ONE);

        // Test
        InventoryResponseDto inventoryResponse = inventoryService.create(new InventoryRequestDto(
                100,
                new ProductRequestDto(
                        "New product name",
                        "New product description",
                        BigDecimal.ONE,
                        expectedProduct.getMerchantId()
                )
        ));

        InventoryEntity newInventory = inventoryRepository.findById(inventoryResponse.id());
        assertNotNull(newInventory);

        assertEquals(expectedProduct.getName(), newInventory.getProduct().getName());
        assertEquals(expectedProduct.getDescription(), newInventory.getProduct().getDescription());
        assertEquals(0, expectedProduct.getPrice().compareTo(newInventory.getProduct().getPrice()), "Price mismatch");
        assertEquals(expectedProduct.getAddedAt().truncatedTo(ChronoUnit.SECONDS),
                newInventory.getProduct().getAddedAt().truncatedTo(ChronoUnit.SECONDS));
        assertEquals(expectedProduct.getStatus(), newInventory.getProduct().getStatus());
        assertEquals(expectedProduct.getMerchantId(), newInventory.getProduct().getMerchantId());
        assertEquals(expectedInventory.getAvailableStock(), newInventory.getAvailableStock());
        assertEquals(expectedInventory.getCreatedAt().truncatedTo(ChronoUnit.SECONDS),
                newInventory.getCreatedAt().truncatedTo(ChronoUnit.SECONDS));
        assertEquals(expectedInventory.getLastUpdatedAt().truncatedTo(ChronoUnit.SECONDS),
                newInventory.getLastUpdatedAt().truncatedTo(ChronoUnit.SECONDS));
    }

    private ProductRequestDto validProduct() {
        return new ProductRequestDto(
                "Valid Name",
                "Valid Description",
                BigDecimal.valueOf(10),
                UUID.randomUUID()
        );
    }

    @Test
    public void testCreateInventory_InventoryWithNegativeStock() {
        InventoryRequestDto dto = new InventoryRequestDto(
                -1,
                validProduct()
        );

        Set<ConstraintViolation<InventoryRequestDto>> violations = validator.validate(dto);
        assertEquals(1, violations.size());

        ConstraintViolation<InventoryRequestDto> violation = violations.iterator().next();
        assertEquals("Available stock cannot be lesser than 0", violation.getMessage());
    }

    @Test
    public void testCreateInventory_InventoryWithNullProduct() {
        InventoryRequestDto dto = new InventoryRequestDto(
                10,
                null // invalid product
        );

        Set<ConstraintViolation<InventoryRequestDto>> violations = validator.validate(dto);
        assertEquals(1, violations.size());

        ConstraintViolation<InventoryRequestDto> violation = violations.iterator().next();
        assertEquals("Product omitted", violation.getMessage());
    }

    @Test
    public void testCreateInventory_ProductWithBlankName() {
        ProductRequestDto product = new ProductRequestDto(
                "",  // invalid
                "Valid Description",
                BigDecimal.valueOf(10),
                UUID.randomUUID()
        );

        Set<ConstraintViolation<ProductRequestDto>> violations = validator.validate(product);
        assertEquals(1, violations.size());

        ConstraintViolation<ProductRequestDto> violation = violations.iterator().next();
        assertEquals("Name omitted", violation.getMessage());
    }

    @Test
    public void testCreateInventory_ProductWithDescriptionTooLong() {
        String longDesc = "x".repeat(501); // exceeds 500
        ProductRequestDto product = new ProductRequestDto(
                "Valid Name",
                longDesc,
                BigDecimal.valueOf(10),
                UUID.randomUUID()
        );

        Set<ConstraintViolation<ProductRequestDto>> violations = validator.validate(product);
        assertEquals(1, violations.size());

        ConstraintViolation<ProductRequestDto> violation = violations.iterator().next();
        assertEquals("Max characters exceeded (200)", violation.getMessage());
    }

    @Test
    public void testCreateInventory_ProductWithPriceLessThanOne() {
        ProductRequestDto product = new ProductRequestDto(
                "Valid Name",
                "Valid Description",
                BigDecimal.ZERO,  // invalid
                UUID.randomUUID()
        );

        Set<ConstraintViolation<ProductRequestDto>> violations = validator.validate(product);
        assertEquals(1, violations.size());

        ConstraintViolation<ProductRequestDto> violation = violations.iterator().next();
        assertEquals("Price must be at least 1", violation.getMessage());
    }

    @Test
    public void testCreateInventory_ValidInventory() {
        InventoryRequestDto dto = new InventoryRequestDto(
                5,
                validProduct()
        );

        Set<ConstraintViolation<InventoryRequestDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void testCreateInventory_MerchantIdMissing() {
        InventoryRequestDto dto = new InventoryRequestDto(
                5,
                new ProductRequestDto(
                        "Valid Name",
                        "Valid Description",
                        BigDecimal.valueOf(10),
                        null
                )
        );
        BadRequestException exception = assertThrows(BadRequestException.class, () -> inventoryService.create(dto));
        assertEquals("Merchant id omitted", exception.getMessage());
    }

    @Transactional
    @Test
    public void testDeleteInventory_Success() {
        // Setup
        InventoryEntity toDelete = getDummyInventory();
        inventoryRepository.persist(toDelete);

        // Test
        inventoryService.delete(toDelete.getId());
        Optional<InventoryEntity> optionalInventory = inventoryRepository.findByIdOptional(toDelete.getId());
        assertTrue(optionalInventory.isEmpty());
    }

    @Transactional
    @Test
    public void testDeleteInventory_InventoryLocked() {
        // Setup
        InventoryEntity toDelete = getDummyInventory();
        toDelete.setLocked(true);
        inventoryRepository.persist(toDelete);
        ClientErrorException exception = assertThrows(ClientErrorException.class, () -> inventoryService.delete(toDelete.getId()));
        assertEquals("Inventory is currently locked", exception.getMessage());
        assertEquals(409, exception.getResponse().getStatus());

        // Cleanup
        inventoryRepository.delete(toDelete);
    }

    @Test
    public void testDeleteInventory_NotFound() {
        UUID id = UUID.fromString("a3c1d2f5-4e6b-ffff-9a2c-1e7d3f6b8c13");
        NotFoundException exception = assertThrows(NotFoundException.class, () -> inventoryService.delete(id));
        assertEquals("Inventory not found", exception.getMessage());
    }

    @Transactional
    @Test
    public void testDiscontinueProduct_Success() {
        InventoryEntity inventory = getDummyInventory();
        inventoryRepository.persist(inventory);

        ProductResponseDto response = inventoryService.discontinueProduct(inventory.getProduct().getId());
        InventoryEntity result = inventoryRepository
                .find("product.id = ?1", response.id())
                .firstResult();
        assertNotNull(result);

        assertEquals(ProductStatus.DISCONTINUED, result.getProduct().getStatus());

        // Cleanup
        inventoryRepository.delete(inventory);
    }

    @Transactional
    @Test
    public void testDiscontinueProduct_InventoryLocked() {
        // Setup
        InventoryEntity inventory = getDummyInventory();
        inventory.setLocked(true);
        inventoryRepository.persist(inventory);
        ClientErrorException exception = assertThrows(ClientErrorException.class, () -> inventoryService.discontinueProduct(inventory.getProduct().getId()));
        assertEquals("Inventory is currently locked", exception.getMessage());
        assertEquals(409, exception.getResponse().getStatus());

        // Cleanup
        inventoryRepository.delete(inventory);
    }

    @Test
    public void testDiscontinueProduct_ProductNotFound() {
        UUID productId = UUID.fromString("a3c1d2f5-4e6b-ffff-9a2c-1e7d3f6b8c13");
        NotFoundException exception = assertThrows(NotFoundException.class, () -> inventoryService.discontinueProduct(productId));
        assertEquals("Product not found", exception.getMessage());
    }

    @Transactional
    @Test
    public void testDiscontinueProduct_ProductDiscontinuedAlready() {
        InventoryEntity inventory = getDummyInventory();
        inventory.getProduct().setStatus(ProductStatus.DISCONTINUED);
        inventoryRepository.persist(inventory);
        BadRequestException exception = assertThrows(BadRequestException.class, () -> inventoryService.discontinueProduct(inventory.getProduct().getId()));
        assertEquals("Product is already discontinued", exception.getMessage());

        // Cleanup
        inventoryRepository.delete(inventory);
    }

    @Transactional
    @Test
    public void testReplenishStock_Success() {
        InventoryEntity inventory = getDummyInventory();
        inventory.setAvailableStock(100);
        inventoryRepository.persist(inventory);

        InventoryResponseDto response = inventoryService.replenishStock(inventory.getId(), 100);
        InventoryEntity result = inventoryRepository.findById(response.id());
        assertNotNull(result);

        assertEquals(200, result.getAvailableStock());

        // Cleanup
        inventoryRepository.delete(inventory);
    }

    @Transactional
    @Test
    public void testReplenishStock_InventoryLocked() {
        // Setup
        InventoryEntity inventory = getDummyInventory();
        inventory.setLocked(true);
        inventoryRepository.persist(inventory);
        ClientErrorException exception = assertThrows(ClientErrorException.class, () -> inventoryService.replenishStock(inventory.getId(), 100));
        assertEquals("Inventory is currently locked", exception.getMessage());
        assertEquals(409, exception.getResponse().getStatus());

        // Cleanup
        inventoryRepository.delete(inventory);
    }

    @Transactional
    @Test
    public void testReplenishStock_InvalidAmount() {
        InventoryEntity inventory = getDummyInventory();
        inventory.setAvailableStock(100);
        inventoryRepository.persist(inventory);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> inventoryService.replenishStock(inventory.getId(), 0));
        assertEquals("Replenish amount must be at least 1", exception.getMessage());

        // Cleanup
        inventoryRepository.delete(inventory);
    }

    @Test
    public void testReplenishStock_NotFound() {
        UUID id = UUID.fromString("a3c1d2f5-4e6b-ffff-9a2c-1e7d3f6b8c13");
        NotFoundException exception = assertThrows(NotFoundException.class, () -> inventoryService.replenishStock(id, 1));
        assertEquals("Inventory not found", exception.getMessage());
    }

    @Transactional
    @Test
    public void testUpdateProduct_Success() {
        // expected values
        InventoryEntity expectedInventory = getDummyInventory();
        inventoryRepository.persist(expectedInventory);
        ProductEntity expectedProduct = expectedInventory.getProduct();


        // Test
        ProductResponseDto productResponse = inventoryService.updateProduct(
                expectedInventory.getProduct().getId(),
                new ProductRequestDto(
                        "New product name",
                        "New product description",
                        BigDecimal.ONE,
                        expectedProduct.getMerchantId()
                )
        );

        InventoryEntity newInventory = inventoryRepository
                .find("product.id = ?1", productResponse.id())
                .firstResult();
        assertNotNull(newInventory);

        assertEquals(expectedProduct.getId(), newInventory.getProduct().getId());
        assertEquals(expectedProduct.getName(), newInventory.getProduct().getName());
        assertEquals(expectedProduct.getDescription(), newInventory.getProduct().getDescription());
        assertEquals(expectedProduct.getPrice(), newInventory.getProduct().getPrice());
        assertEquals(expectedProduct.getAddedAt(), newInventory.getProduct().getAddedAt());
        assertEquals(expectedProduct.getStatus(), newInventory.getProduct().getStatus());
        assertEquals(expectedProduct.getMerchantId(), newInventory.getProduct().getMerchantId());
        assertEquals(expectedInventory.getId(), newInventory.getId());
        assertEquals(expectedInventory.getAvailableStock(), newInventory.getAvailableStock());
        assertEquals(expectedInventory.getCreatedAt(), newInventory.getCreatedAt());
        assertEquals(expectedInventory.getLastUpdatedAt(), newInventory.getLastUpdatedAt());

        // Cleanup
        inventoryRepository.delete(expectedInventory);
    }

    @Transactional
    @Test
    public void testUpdateProduct_InventoryLocked() {
        // expected values
        InventoryEntity expectedInventory = getDummyInventory();
        expectedInventory.setLocked(true);
        inventoryRepository.persist(expectedInventory);
        ProductEntity expectedProduct = expectedInventory.getProduct();

        ClientErrorException exception = assertThrows(ClientErrorException.class, () ->
                inventoryService.updateProduct(
                        expectedInventory.getProduct().getId(),
                        new ProductRequestDto(
                                "New product name",
                                "New product description",
                                BigDecimal.ONE,
                                expectedProduct.getMerchantId()
                        )
                ));
        assertEquals("Inventory is currently locked", exception.getMessage());
        assertEquals(409, exception.getResponse().getStatus());

        // Cleanup
        inventoryRepository.delete(expectedInventory);
    }

    @Transactional
    @Test
    public void testUpdateProduct_ProductDiscontinued() {
        InventoryEntity inventory = getDummyInventory();
        inventory.getProduct().setStatus(ProductStatus.DISCONTINUED);
        inventoryRepository.persist(inventory);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> inventoryService.updateProduct(inventory.getProduct().getId(), validProduct()));
        assertEquals("Cannot update discontinued product", exception.getMessage());

        // Cleanup
        inventoryRepository.delete(inventory);
    }

    @Test
    public void testUpdateProduct_NotFound() {
        UUID id = UUID.fromString("a3c1d2f5-4e6b-ffff-9a2c-1e7d3f6b8c13");
        NotFoundException exception = assertThrows(NotFoundException.class, () -> inventoryService.updateProduct(id, validProduct()));
        assertEquals("Product not found", exception.getMessage());
    }

}



