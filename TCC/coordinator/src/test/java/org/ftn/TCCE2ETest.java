package org.ftn;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.ftn.constant.CoordinatorTransactionState;
import org.ftn.constant.Decision;
import org.ftn.dto.CoordinatorTransactionDto;
import org.ftn.dto.CreateOrderRequestDto;
import org.ftn.resource.IdentityProviderTestContainer;
import org.ftn.resource.WebShopServicesTestContainersManager;
import org.ftn.service.CoordinatorService;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(WebShopServicesTestContainersManager.class)
@QuarkusTestResource(IdentityProviderTestContainer.class)
public class TCCE2ETest {
    @Inject
    CoordinatorService coordinatorService;

    @Test
    public void testTCCTransaction_Success() {
        UUID idempotencyKey = UUID.randomUUID();
        UUID productId = UUID.fromString("d572df76-b527-4e31-8aa3-9aa954d17100");
        UUID userId = UUID.fromString("19e5ddb1-4c66-4d17-ad06-e8a6af23ed58");
        int amount = 3;
        CreateOrderRequestDto request = new CreateOrderRequestDto(productId, amount);
        CoordinatorTransactionDto startedTx = coordinatorService.createTransaction(request, userId);

        await()
                .atMost(10, TimeUnit.MINUTES)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> {
                    CoordinatorTransactionDto tx = coordinatorService.getTransaction(startedTx.id());
                    System.out.println(">>> Tx: " + tx);
                    return tx.state().equals(CoordinatorTransactionState.COMMITTED)
                            || tx.state().equals(CoordinatorTransactionState.ABORTED);
                });

        CoordinatorTransactionDto finishedTx = coordinatorService.getTransaction(startedTx.id());
        assertEquals(CoordinatorTransactionState.COMMITTED, finishedTx.state());
        assertEquals(Decision.COMMIT, finishedTx.decision());
    }

    @Test
    public void testTCCTransaction_RollbackInventory() {
        UUID idempotencyKey = UUID.randomUUID();
        UUID productId = UUID.fromString("d572df76-b527-4e31-bbbb-9aa954d17100");   // Non-existent product
        UUID userId = UUID.fromString("19e5ddb1-4c66-4d17-ad06-e8a6af23ed58");
        int amount = 3;
        CreateOrderRequestDto request = new CreateOrderRequestDto(productId, amount);
        CoordinatorTransactionDto startedTx = coordinatorService.createTransaction(request, userId);

        await()
                .atMost(1, TimeUnit.MINUTES)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> {
                    CoordinatorTransactionDto tx = coordinatorService.getTransaction(startedTx.id());
                    System.out.println(">>> Tx: " + tx);
                    return tx.state().equals(CoordinatorTransactionState.COMMITTED)
                            || tx.state().equals(CoordinatorTransactionState.ABORTED);
                });

        CoordinatorTransactionDto finishedTx = coordinatorService.getTransaction(startedTx.id());
        assertEquals(CoordinatorTransactionState.ABORTED, finishedTx.state());
        assertEquals(Decision.ABORT, finishedTx.decision());
        assertEquals("Product not found", finishedTx.abortReason());
    }

    @Test
    public void testTCCTransaction_RollbackPayment() {
        UUID idempotencyKey = UUID.randomUUID();
        UUID productId = UUID.fromString("d572df76-b527-4e31-8aa3-9aa954d17104");
        UUID userId = UUID.fromString("19e5ddb1-4c66-4d17-ad06-e8a6af23ed58");
        int amount = 10;    // Insufficient balance
        CreateOrderRequestDto request = new CreateOrderRequestDto(productId, amount);
        CoordinatorTransactionDto startedTx = coordinatorService.createTransaction(request, userId);

        await()
                .atMost(1, TimeUnit.MINUTES)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> {
                    CoordinatorTransactionDto tx = coordinatorService.getTransaction(startedTx.id());
                    System.out.println(">>> Tx: " + tx);
                    return tx.state().equals(CoordinatorTransactionState.COMMITTED)
                            || tx.state().equals(CoordinatorTransactionState.ABORTED);
                });

        CoordinatorTransactionDto finishedTx = coordinatorService.getTransaction(startedTx.id());
        assertEquals(CoordinatorTransactionState.ABORTED, finishedTx.state());
        assertEquals(Decision.ABORT, finishedTx.decision());
        assertEquals("Insufficient balance", finishedTx.abortReason());
    }


}
