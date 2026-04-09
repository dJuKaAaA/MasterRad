package org.ftn.service;

import io.quarkus.agroal.DataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.ftn.order.constant.OrderStatus;
import org.ftn.order.dto.OrderRequestDto;
import org.ftn.order.dto.OrderResponseDto;
import org.ftn.order.entity.OrderEntity;
import org.ftn.order.repository.OrderRepository;
import org.ftn.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class OrderServiceTest {
    @Inject
    OrderService orderSagaService;
    @Inject
    @DataSource("order")
    OrderRepository orderRepository;
    @Inject
    Validator validator;

    @Test
    public void testCreateOrder_Success() {
        long ordersCount = orderRepository.count();

        OrderRequestDto orderRequestDto = new OrderRequestDto(UUID.randomUUID(), 5, UUID.randomUUID());
        OrderResponseDto orderResponseDto = orderSagaService.createOrder(orderRequestDto);

        long newOrdersCount = orderRepository.count();
        assertEquals(ordersCount + 1, newOrdersCount, "Order should be persisted in DB");

        OrderEntity order = orderRepository.findById(orderResponseDto.id());
        assertNotNull(order);

        assertEquals(orderRequestDto.productId(), orderResponseDto.productId(), "Product ID between request and response mismatch");
        assertEquals(orderRequestDto.quantity(), orderResponseDto.quantity(), "Quantity between request and response mismatch");
        assertEquals(orderRequestDto.userId(), orderResponseDto.userId(), "User ID between request and response mismatch");
        assertEquals(OrderStatus.COMPLETE, orderResponseDto.status(), "Status between request and response mismatch");

        assertEquals(orderRequestDto.productId(), order.getProductId(), "Product ID between request and entity mismatch");
        assertEquals(orderRequestDto.quantity(), order.getQuantity(), "Quantity between request and entity mismatch");
        assertEquals(orderRequestDto.userId(), order.getUserId(), "User ID between request and entity mismatch");
        assertEquals(OrderStatus.COMPLETE, order.getStatus(), "Status between request and entity mismatch");
    }

    @ParameterizedTest
    @CsvSource(value = {
            "N/A, 2, 8cca7a29-5add-4197-ad56-48be327ea13c, Product id omitted",
            "d572df76-b527-4e31-8aa3-9aa954d17100, N/A, 8cca7a29-5add-4197-ad56-48be327ea13c, Quantity omitted",
            "d572df76-b527-4e31-8aa3-9aa954d17100, 2, N/A, User id omitted",

            "N/A, N/A, 8cca7a29-5add-4197-ad56-48be327ea13c, Product id omitted;Quantity omitted",
            "d572df76-b527-4e31-8aa3-9aa954d17100, N/A, N/A, Quantity omitted;User id omitted",
            "N/A, 2, N/A, Product id omitted;User id omitted",

            "N/A, N/A, N/A, Product id omitted;Quantity omitted;User id omitted"
    }, nullValues = {"N/A"})
    public void testCreateOrder_FailMissingDtoFields(UUID productId, Integer quantity, UUID userId, String messages) {
        OrderRequestDto orderRequestDto = new OrderRequestDto(productId, quantity, userId);
        Set<ConstraintViolation<OrderRequestDto>> violations = validator.validate(orderRequestDto);
        String[] messagesArray = messages.split(";");
        List<String> violationMessages = violations.stream().map(ConstraintViolation::getMessage).toList();

        assertFalse(violations.isEmpty());

        for (String message : messagesArray) {
            assertTrue(violationMessages.contains(message));
        }
    }

    @Test
    public void testCreateOrder_FailInvalidQuantity() {
        Integer quantity = 0;
        OrderRequestDto orderRequestDto = new OrderRequestDto(UUID.randomUUID(), quantity, UUID.randomUUID());
        Set<ConstraintViolation<OrderRequestDto>> violations = validator.validate(orderRequestDto);

        assertEquals(1, violations.size());
        String message = violations.iterator().next().getMessage();
        assertEquals("Quantity must be at least 1", message);
    }

    @Test
    public void testCancelOrder_Success() {
        System.out.println("Count of orders: " + orderRepository.count());
        UUID orderId = UUID.fromString("03b229a1-0529-4a4a-a920-a7dda2637f70");
        OrderEntity order = orderRepository.findById(orderId);

        assertEquals(OrderStatus.COMPLETE, order.getStatus());

        orderSagaService.cancelOrder(order.getId());

        orderRepository.getEntityManager().clear();

        OrderEntity canceledOrder = orderRepository.findById(order.getId());

        assertNotNull(canceledOrder);
        assertEquals(OrderStatus.CANCELED, canceledOrder.getStatus());
    }
}
