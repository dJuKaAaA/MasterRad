package org.ftn.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.ftn.dto.PageResponse;
import org.ftn.dto.PaymentResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
public class PaymentServiceTest {
    @Inject
    PaymentService paymentService;

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
        PageResponse<PaymentResponseDto> results = paymentService.getAll(page, size);
        assertEquals(expectedPage, results.page(), "Expected page mismatch");
        assertEquals(expectedSize, results.size(), "Expected size mismatch");
        assertEquals(expectedTotalPages, results.totalPages(), "Expected total pages mismatch");
        assertEquals(expectedTotalSize, results.totalSize(), "Expected total size mismatch");
    }

    @ParameterizedTest
    @CsvSource({
            "8cca7a29-5add-4197-ad56-48be327ea13c, 0, 5, 0, 5, 1, 5",
            "8cca7a29-5add-4197-ad56-48be327ea13c, 0, 10, 0, 5, 1, 5",
            "8cca7a29-5add-4197-ad56-48be327ea13c, 1, 2, 1, 2, 3, 5",
            "8cca7a29-5add-4197-ad56-48be327ea13c, 1, 3, 1, 2, 2, 5",
            "8cca7a29-5add-4197-ad56-48be327ea13c, 2, 5, 2, 0, 1, 5",
            "8cca7a29-5add-ffff-ad56-43be327ea13d, 0, 5, 0, 0, 1, 0"
    })
    public void testGetAllByUserId(UUID merchantId,
                                   int page,
                                   int size,
                                   int expectedPage,
                                   int expectedSize,
                                   int expectedTotalPages,
                                   long expectedTotalSize) {
        PageResponse<PaymentResponseDto> results = paymentService.getAll(merchantId, page, size);
        assertEquals(expectedPage, results.page(), "Expected page mismatch");
        assertEquals(expectedSize, results.size(), "Expected size mismatch");
        assertEquals(expectedTotalPages, results.totalPages(), "Expected total pages mismatch");
        assertEquals(expectedTotalSize, results.totalSize(), "Expected total size mismatch");
    }

    @Test
    public void testGetById_Success() {
        UUID id = UUID.fromString("e3b2c1d4-7f5a-4c3b-8d1e-9b2c3a4f5d60");
        PaymentResponseDto payment = paymentService.get(id);
        assertEquals(id, payment.id());
    }

    @Test
    public void testGetById_NotFound() {
        UUID id = UUID.fromString("b3c1d2f5-4e6b-4b3f-9a2c-1e7d3f6b8c13");
        NotFoundException exception = assertThrows(NotFoundException.class, () -> paymentService.get(id));
        assertEquals("Payment not found", exception.getMessage());
    }

    @Test
    public void testGetByIdAndMerchantId_Success() {
        UUID id = UUID.fromString("e3b2c1d4-7f5a-4c3b-8d1e-9b2c3a4f5d60");
        UUID userId = UUID.fromString("8cca7a29-5add-4197-ad56-48be327ea13c");
        PaymentResponseDto payment = paymentService.get(id, userId);
        assertEquals(id, payment.id());
        assertEquals(userId, payment.userId());
    }

    @Test
    public void testGetByIdAndMerchantId_NotFound() {
        UUID id = UUID.fromString("b3c1d2f5-4e6b-4b3f-9a2c-1e7d3f6b8c13");
        UUID userId = UUID.fromString("8cca7a29-5add-4197-ad56-48be327ea13c");
        NotFoundException exception = assertThrows(NotFoundException.class, () -> paymentService.get(id, userId));
        assertEquals("Payment not found", exception.getMessage());
    }

    @Test
    public void testGetByIdAndMerchantId_MerchantMismatch() {
        UUID id = UUID.fromString("e3b2c1d4-7f5a-4c3b-8d1e-9b2c3a4f5d60");
        UUID merchantId = UUID.fromString("7e865ca7-a38e-4000-0000-fa6d01e9bdbf");
        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> paymentService.get(id, merchantId));
        assertEquals("User id mismatch for payment", exception.getMessage());
    }
}
