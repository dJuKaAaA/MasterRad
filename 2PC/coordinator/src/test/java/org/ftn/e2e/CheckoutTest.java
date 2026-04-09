package org.ftn.e2e;

import io.quarkus.agroal.DataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.ftn.dto.CheckoutErrorDto;
import org.ftn.dto.CreateOrderRequestDto;
import org.ftn.inventory.repository.InventoryRepository;
import org.ftn.mapper.CheckoutErrorMapper;
import org.ftn.order.constant.OrderStatus;
import org.ftn.order.repository.OrderRepository;
import org.ftn.payment.constant.PaymentStatus;
import org.ftn.payment.repository.PaymentRepository;
import org.ftn.payment.repository.WalletRepository;
import org.ftn.repository.CheckoutErrorRepository;
import org.ftn.service.CheckoutErrorService;
import org.ftn.service.CoordinatorService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class CheckoutTest {
    @Inject
    @DataSource("order")
    OrderRepository orderRepository;
    @Inject
    @DataSource("inventory")
    InventoryRepository inventoryRepository;
    @Inject
    @DataSource("payment")
    PaymentRepository paymentRepository;
    @Inject
    @DataSource("payment")
    WalletRepository walletRepository;

    @Inject
    CheckoutErrorRepository checkoutErrorRepository;
    @Inject
    CheckoutErrorMapper checkoutErrorMapper;

    @Inject
    CoordinatorService coordinatorService;
    @Inject
    CheckoutErrorService checkoutErrorService;

    @Inject
    ManagedExecutor executor;

    /*
    MOCKS HOW THE CHECKOUT ENDPOINT WORKS SO I CAN JUST TEST THE METHOD AND NOT THE ENDPOINT (because there are complications with testing the JsonWebToken)
     */
    public void checkoutMock(CreateOrderRequestDto request, UUID userId) {
        executor.runAsync(() -> {
            try {
                coordinatorService.createOrder(request, userId);
            } catch (Exception e) {
                System.out.printf("Something went wrong with the checkout: %s\n", ExceptionUtils.getRootCauseMessage(e));
                if (e instanceof WebApplicationException we) {
                    checkoutErrorService.save(request, userId, we.getMessage(), we.getResponse().getStatus());
                }
            }
        });

    }

    @Test
    public void testCheckout_Success() {
        UUID userId = UUID.fromString("daa45fd6-3500-4a0d-914d-052082303122");
        UUID productId = UUID.fromString("d572df76-b527-4e31-8aa3-9aa954d17100");
        int amount = 5;
        CreateOrderRequestDto request = new CreateOrderRequestDto(productId, amount);

        BigDecimal productPrice = getProductPrice(productId);
        int previousStock = getInventoryStock(productId);
        BigDecimal previousBalance = getUserBalance(userId);

        checkoutMock(request, userId);

        await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(3, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        return getPaymentStatus(amount, productId, userId) == PaymentStatus.SUCCESS;
                    } catch (NotFoundException e) {
                        return false;
                    }
                });

        OrderStatus orderStatus = getOrderStatus(amount, productId, userId);
        int currentStock = getInventoryStock(productId);
        BigDecimal currentBalance = getUserBalance(userId);

        assertEquals(OrderStatus.COMPLETE, orderStatus);
        assertEquals(previousStock - amount, currentStock);
        assertEquals(previousBalance.subtract(productPrice.multiply(BigDecimal.valueOf(amount))).setScale(2, RoundingMode.HALF_UP),
                currentBalance.setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    public void testCheckout_RollbackInventory() {
        UUID userId = UUID.fromString("daa45fd6-3500-4a0d-914d-052082303122");
        UUID productId = UUID.fromString("d572df76-b527-4e31-bbbb-9aa954d17100");   // Non-existent inventory
        int amount = 5;
        CreateOrderRequestDto request = new CreateOrderRequestDto(productId, amount);

        checkoutMock(request, userId);

        await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(3, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        return getCheckoutError(amount, productId, userId) != null;
                    } catch (NotFoundException e) {
                        return false;
                    }
                });

        CheckoutErrorDto checkoutError = getCheckoutError(amount, productId, userId);

        assertEquals("Inventory not found", checkoutError.message());
        assertEquals(404, checkoutError.status());
    }

    @Test
    public void testCheckout_RollbackPayment() {
        UUID userId = UUID.fromString("daa45fd6-3500-4a0d-914d-052082303122");
        UUID productId = UUID.fromString("d572df76-b527-4e31-8aa3-9aa954d17104");
        int amount = 15;    // Insufficient balance
        CreateOrderRequestDto request = new CreateOrderRequestDto(productId, amount);

        checkoutMock(request, userId);

        await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(3, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        return getCheckoutError(amount, productId, userId) != null;
                    } catch (NotFoundException e) {
                        return false;
                    }
                });

        CheckoutErrorDto checkoutError = getCheckoutError(amount, productId, userId);

        assertEquals("Insufficient balance", checkoutError.message());
        assertEquals(400, checkoutError.status());
    }

    @Transactional
    public OrderStatus getOrderStatus(int amount, UUID productId, UUID userId) {
        return orderRepository
                .find("quantity = ?1 and productId = ?2 and userId = ?3", amount, productId, userId)
                .firstResultOptional()
                .orElseThrow(() -> new NotFoundException("Order not found while testing"))
                .getStatus();
    }

    @Transactional
    public int getInventoryStock(UUID productId) {
        return inventoryRepository
                .find("product.id = ?1", productId)
                .firstResultOptional()
                .orElseThrow(() -> new NotFoundException("Inventory not found while testing"))
                .getAvailableStock();
    }

    @Transactional
    public BigDecimal getProductPrice(UUID productId) {
        return inventoryRepository
                .find("product.id = ?1", productId)
                .firstResultOptional()
                .orElseThrow(() -> new NotFoundException("Inventory not found while testing"))
                .getProduct()
                .getPrice();
    }

    @Transactional
    public PaymentStatus getPaymentStatus(int amount, UUID productId, UUID userId) {
        return paymentRepository
                .find("productQuantity = ?1 and productId = ?2 and payer.userId = ?3", amount, productId, userId)
                .firstResultOptional()
                .orElseThrow(() -> new NotFoundException("Payment not found while testing"))
                .getStatus();
    }

    @Transactional
    public BigDecimal getUserBalance(UUID userId) {
        return walletRepository
                .find("userId = ?1", userId)
                .firstResultOptional()
                .orElseThrow(() -> new NotFoundException("Wallet not found while testing"))
                .getBalance();
    }

    @Transactional
    public CheckoutErrorDto getCheckoutError(int amount, UUID productId, UUID userId) {
        return checkoutErrorRepository
                .find("amount = ?1 and productId = ?2 and userId = ?3", amount, productId, userId)
                .firstResultOptional()
                .map(checkoutErrorMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Checkout error not found while testing"));
    }

}
