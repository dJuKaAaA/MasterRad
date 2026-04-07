package org.ftn;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.ftn.constant.SagaState;
import org.ftn.constant.TransactionState;
import org.ftn.dto.CreateOrderRequestDto;
import org.ftn.dto.SagaResponseDto;
import org.ftn.resource.KafkaTestContainerManager;
import org.ftn.resource.WebShopServicesTestContainersManager;
import org.ftn.service.KafkaSagaService;
import org.ftn.service.SagaService;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(KafkaTestContainerManager.class)
@QuarkusTestResource(WebShopServicesTestContainersManager.class)
public class SagaE2ETest {
    @Inject
    KafkaSagaService kafkaSagaService;
    @Inject
    SagaService sagaService;

    @Test
    public void testSagaTransaction_Success() {
        UUID idempotencyKey = UUID.randomUUID();
        UUID productId = UUID.fromString("d572df76-b527-4e31-8aa3-9aa954d17100");
        UUID userId = UUID.fromString("8cca7a29-5add-4197-ad56-48be327ea13c");
        int amount = 2;
        CreateOrderRequestDto request = new CreateOrderRequestDto(productId, amount, idempotencyKey.toString());
        SagaResponseDto sagaResponseDto = kafkaSagaService.createOrderTransactionKafka(idempotencyKey, request, userId);

        await()
                .atMost(1, TimeUnit.MINUTES)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> {
                    SagaResponseDto saga = sagaService.getSaga(UUID.fromString(sagaResponseDto.id()));
                    System.out.println(">>> Saga: " + saga);
                    return saga.state().equals(SagaState.COMPLETED.name())
                            || saga.state().equals(SagaState.FAILED.name());
                });
        SagaResponseDto saga = sagaService.getSaga(UUID.fromString(sagaResponseDto.id()));
        assertEquals(SagaState.COMPLETED.name(), saga.state(), "Saga not completed successfully - State is %s".formatted(saga.state()));
        assertEquals(TransactionState.COMMITTING.name(), saga.transactionState());
    }

    @Test
    public void testSagaTransaction_InventoryRollback() {
        UUID idempotencyKey = UUID.randomUUID();
        UUID productId = UUID.fromString("d572df76-b527-4e31-8aa3-9aa954d17104");
        UUID userId = UUID.fromString("8cca7a29-5add-4197-ad56-48be327ea13c");
        int amount = 10;    // Insufficient balance
        CreateOrderRequestDto request = new CreateOrderRequestDto(productId, amount, idempotencyKey.toString());
        SagaResponseDto sagaResponseDto = kafkaSagaService.createOrderTransactionKafka(idempotencyKey, request, userId);

        await()
                .atMost(1, TimeUnit.MINUTES)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> {
                    SagaResponseDto saga = sagaService.getSaga(UUID.fromString(sagaResponseDto.id()));
                    System.out.println(">>> Saga: " + saga);
                    return saga.state().equals(SagaState.COMPLETED.name())
                            || saga.state().equals(SagaState.FAILED.name());
                });
        SagaResponseDto saga = sagaService.getSaga(UUID.fromString(sagaResponseDto.id()));
        assertEquals(SagaState.FAILED.name(), saga.state(), "Saga completed successfully - State is %s".formatted(saga.state()));
        assertEquals("Insufficient balance", saga.failureReason());
        assertEquals(TransactionState.ROLLING_BACK.name(), saga.transactionState());
    }

    @Test
    public void testSagaTransaction_OrderRollback() {
        UUID idempotencyKey = UUID.randomUUID();
        UUID productId = UUID.fromString("d572df76-cccc-4e31-8aa3-9aa954d17100");   // Non-existent product
        UUID userId = UUID.fromString("8cca7a29-5add-4197-ad56-48be327ea13c");
        int amount = 2;
        CreateOrderRequestDto request = new CreateOrderRequestDto(productId, amount, idempotencyKey.toString());
        SagaResponseDto sagaResponseDto = kafkaSagaService.createOrderTransactionKafka(idempotencyKey, request, userId);

        await()
                .atMost(1, TimeUnit.MINUTES)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> {
                    SagaResponseDto saga = sagaService.getSaga(UUID.fromString(sagaResponseDto.id()));
                    System.out.println(">>> Saga: " + saga);
                    return saga.state().equals(SagaState.COMPLETED.name())
                            || saga.state().equals(SagaState.FAILED.name());
                });
        SagaResponseDto saga = sagaService.getSaga(UUID.fromString(sagaResponseDto.id()));
        assertEquals(SagaState.FAILED.name(), saga.state(), "Saga completed successfully - State is %s".formatted(saga.state()));
        assertEquals("Inventory not found", saga.failureReason());
        assertEquals(TransactionState.ROLLING_BACK.name(), saga.transactionState());
    }
}