package org.ftn.service.test2;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.ftn.constant.OrderStatus;
import org.ftn.dto.*;
import org.ftn.entity.OrderEntity;
import org.ftn.repository.OrderRepository;
import org.ftn.service.OrderSagaService;
import org.ftn.util.KafkaTestResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(KafkaTestResource.class)
public class OrderSagaServiceTest {
    @Inject
    OrderSagaService orderSagaService;
    @Inject
    OrderRepository orderRepository;
    @Inject
    Validator validator;
    @Inject
    @Any
    InMemoryConnector connector;

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
        UUID orderId = UUID.fromString("03b229a1-0529-4a4a-a920-a7dda2637f70");
        OrderEntity order = orderRepository.findById(orderId);

        assertEquals(OrderStatus.COMPLETE, order.getStatus());

        orderSagaService.cancelOrder(order.getId());

        orderRepository.getEntityManager().clear();

        OrderEntity canceledOrder = orderRepository.findById(order.getId());

        assertNotNull(canceledOrder);
        assertEquals(OrderStatus.CANCELED, canceledOrder.getStatus());
    }

    @Test
    public void testCreateOrderSagaFlow() {
        // 1. Prepare the Mock Incoming Message
        InMemorySource<KafkaOrderRequestDto> commitSource = connector.source("order-service-commit");
        InMemorySink<KafkaOrderResponseDto> responseSink = connector.sink("order-service-response");

        OrderRequestDto requestDto = new OrderRequestDto(UUID.randomUUID(), 100, UUID.randomUUID());
        KafkaOrderRequestDto payload = new KafkaOrderRequestDto(requestDto.userId(), UUID.randomUUID(), requestDto);

        long ordersCount = orderRepository.count();

        // 2. Send the message to the @Incoming channel
        commitSource.send(payload);

        // 3. Assertions: Database State
        await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> responseSink.received().size() == 1);


        List<OrderEntity> orders = orderRepository.listAll();
        assertEquals(ordersCount + 1, orders.size(), "Order should be persisted in DB");
        assertEquals(OrderStatus.COMPLETE, orders.getFirst().getStatus());

        // 4. Assertions: Outgoing Kafka Event
        assertEquals(1, responseSink.received().size(), "Should have sent 1 response message");
        KafkaOrderResponseDto response = responseSink.received().getFirst().getPayload();
        assertEquals(payload.sagaId(), response.sagaId(), "Saga ID must match for correlation");
        assertEquals(payload.userId(), response.userId(), "User ID must match for correlation");
        assertNotNull(response.orderResponseDto(), "Order response should be populated");

        orderRepository.getEntityManager().clear();

        OrderEntity completeOrder = orderRepository.findById(response.orderResponseDto().id());

        assertEquals(requestDto.productId(), response.orderResponseDto().productId(), "Product ID request and response mismatch");
        assertEquals(requestDto.quantity(), response.orderResponseDto().quantity(), "Quantity request and response mismatch");
        assertEquals(requestDto.userId(), response.orderResponseDto().userId(), "User ID request and response mismatch");
        assertEquals(requestDto.productId(), completeOrder.getProductId(), "Product ID request and entity mismatch");
        assertEquals(requestDto.quantity(), completeOrder.getQuantity(), "Quantity request and entity mismatch");
        assertEquals(requestDto.userId(), completeOrder.getUserId(), "User ID request and entity mismatch");
    }

    @Test
    public void testCancelOrderSagaFlow() {
        InMemorySource<KafkaOrderErrorDto> rollbackSource = connector.source("order-service-rollback");
        InMemorySink<KafkaOrderErrorDto> errorSink = connector.sink("order-service-error");

        // Pre-persist an order to cancel
        UUID orderId = UUID.fromString("03b229a1-0529-4a4a-a920-a7dda2637f71");
        // ... logic to save orderId to DB manually or via repository ...

        KafkaOrderErrorDto errorPayload = new KafkaOrderErrorDto(orderId, UUID.randomUUID(), new KafkaErrorDto("Order failed", 400));

        rollbackSource.send(errorPayload);

        await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> errorSink.received().size() == 1);

        // Verify state is CANCELED and error was forwarded
        assertEquals(1, errorSink.received().size());

        OrderEntity canceledOrder = orderRepository.findById(orderId);
        assertNotNull(canceledOrder);

        assertEquals(OrderStatus.CANCELED, canceledOrder.getStatus());
    }
}
