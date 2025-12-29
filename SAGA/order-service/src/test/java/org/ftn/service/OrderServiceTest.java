package org.ftn.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.ftn.constant.OrderStatus;
import org.ftn.dto.OrderResponseDto;
import org.ftn.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
public class OrderServiceTest {
    @Inject
    OrderService orderService;

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
        PageResponse<OrderResponseDto> results = orderService.getAll(page, size);
        assertEquals(expectedPage, results.page(), "Expected page mismatch");
        assertEquals(expectedSize, results.size(), "Expected size mismatch");
        assertEquals(expectedTotalPages, results.totalPages(), "Expected total pages mismatch");
        assertEquals(expectedTotalSize, results.totalSize(), "Expected total size mismatch");
    }

    @ParameterizedTest
    @CsvSource({
            "COMPLETE, 0, 5, 0, 5, 1, 5",
            "COMPLETE, 0, 10, 0, 5, 1, 5",
            "COMPLETE, 1, 2, 1, 2, 3, 5",
            "COMPLETE, 1, 3, 1, 2, 2, 5",
            "COMPLETE, 2, 5, 2, 0, 1, 5",
            "PENDING, 0, 5, 0, 0, 1, 0"
    })
    public void testGetAllByStatus(OrderStatus status,
                                   int page,
                                   int size,
                                   int expectedPage,
                                   int expectedSize,
                                   int expectedTotalPages,
                                   long expectedTotalSize) {
        PageResponse<OrderResponseDto> results = orderService.getAll(page, size, status);
        assertEquals(expectedPage, results.page(), "Expected page mismatch");
        assertEquals(expectedSize, results.size(), "Expected size mismatch");
        assertEquals(expectedTotalPages, results.totalPages(), "Expected total pages mismatch");
        assertEquals(expectedTotalSize, results.totalSize(), "Expected total size mismatch");
    }

    @Test
    public void testGetById_Success() {
        UUID id = UUID.fromString("03b229a1-0529-4a4a-a920-a7dda2637f70");
        OrderResponseDto order = orderService.get(id);
        assertEquals(id, order.id());
    }

    @Test
    public void testGetById_NotFound() {
        UUID id = UUID.fromString("03b229a1-0329-4a4a-a920-a7dda2637f70");
        NotFoundException exception = assertThrows(NotFoundException.class, () -> orderService.get(id));
        assertEquals("Order not found", exception.getMessage());
        assertEquals(404, exception.getResponse().getStatus());
    }

    @Test
    public void testGetByIdAndUserId_Success() {
        UUID id = UUID.fromString("03b229a1-0529-4a4a-a920-a7dda2637f70");
        UUID userId = UUID.fromString("8cca7a29-5add-4197-ad56-48be327ea13c");
        OrderResponseDto order = orderService.get(id, userId);
        assertEquals(id, order.id());
        assertEquals(userId, order.userId());
    }

    @Test
    public void testGetByIdAndUserId_NotFound() {
        UUID id = UUID.fromString("03b229a1-0329-4a4a-a920-a7dda2637f70");
        UUID userId = UUID.fromString("8cca7a29-5add-4197-ad56-48be327ea13c");
        NotFoundException exception = assertThrows(NotFoundException.class, () -> orderService.get(id, userId));
        assertEquals("Order not found", exception.getMessage());
        assertEquals(404, exception.getResponse().getStatus());
    }

    @Test
    public void testGetByIdAndUserId_MismatchUsers() {
        UUID id = UUID.fromString("03b229a1-0529-4a4a-a920-a7dda2637f70");
        UUID userId = UUID.fromString("8cba7a29-5add-4197-ad56-48be327ea13c");
        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> orderService.get(id, userId));
        assertEquals("User id mismatch for order", exception.getMessage());
        assertEquals(403, exception.getResponse().getStatus());

    }

    @ParameterizedTest
    @CsvSource({
            "8cca7a29-5add-4197-ad56-48be327ea13c, 0, 5, 0, 5, 1, 5",
            "8cca7a29-5add-4197-ad56-48be327ea13c, 0, 10, 0, 5, 1, 5",
            "8cca7a29-5add-4197-ad56-48be327ea13c, 1, 2, 1, 2, 3, 5",
            "8cca7a29-5add-4197-ad56-48be327ea13c, 1, 3, 1, 2, 2, 5",
            "8cca7a29-5add-4197-ad56-48be327ea13c, 2, 5, 2, 0, 1, 5",
            "8cca7a29-5add-4197-ad56-43be327ea13c, 0, 5, 0, 0, 1, 0"
    })
    public void testGetAllByUserId(UUID userId,
                                   int page,
                                   int size,
                                   int expectedPage,
                                   int expectedSize,
                                   int expectedTotalPages,
                                   long expectedTotalSize) {
        PageResponse<OrderResponseDto> results = orderService.getAllByUserId(userId, page, size);
        assertEquals(expectedPage, results.page(), "Expected page mismatch");
        assertEquals(expectedSize, results.size(), "Expected size mismatch");
        assertEquals(expectedTotalPages, results.totalPages(), "Expected total pages mismatch");
        assertEquals(expectedTotalSize, results.totalSize(), "Expected total size mismatch");
    }

    @ParameterizedTest
    @CsvSource({
            "d572df76-b527-4e31-8aa3-9aa954d17100, 0, 5, 0, 1, 1, 1",
            "d572df76-b527-4e31-8aa3-9aa954d17100, 0, 10, 0, 1, 1, 1",
            "d572df76-b527-4e31-aaa3-9aa954d17100, 0, 5, 0, 0, 1, 0"
    })
    public void testGetAllByProductId(UUID productId,
                                      int page,
                                      int size,
                                      int expectedPage,
                                      int expectedSize,
                                      int expectedTotalPages,
                                      long expectedTotalSize) {
        PageResponse<OrderResponseDto> results = orderService.getAllByProductId(productId, page, size);
        assertEquals(expectedPage, results.page(), "Expected page mismatch");
        assertEquals(expectedSize, results.size(), "Expected size mismatch");
        assertEquals(expectedTotalPages, results.totalPages(), "Expected total pages mismatch");
        assertEquals(expectedTotalSize, results.totalSize(), "Expected total size mismatch");
    }
}
