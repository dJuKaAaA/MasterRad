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
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.ftn.constant.PaymentStatus;
import org.ftn.dto.*;
import org.ftn.entity.PaymentEntity;
import org.ftn.entity.WalletEntity;
import org.ftn.repository.PaymentRepository;
import org.ftn.repository.WalletRepository;
import org.ftn.service.impl.PaymentSagaServiceImpl;
import org.ftn.util.KafkaTestResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(KafkaTestResource.class)
public class PaymentSagaServiceTest {
    @Inject
    PaymentSagaServiceImpl paymentSagaService;
    @Inject
    PaymentRepository paymentRepository;
    @Inject
    WalletRepository walletRepository;
    @Inject
    Validator validator;
    @Inject
    @Any
    InMemoryConnector connector;

    private UUID getUserId() {
        return UUID.fromString("8cca7a29-5add-4197-ad56-48be327ea13c");
    }

    @Test
    public void testProcess_Success() {
        UUID userId = getUserId();
        WalletEntity wallet = walletRepository.find("userId", userId).firstResult();
        BigDecimal oldBalance = wallet.getBalance();

        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(BigDecimal.valueOf(100), UUID.randomUUID(), 10, userId);

        PaymentResponseDto paymentResponseDto = paymentSagaService.process(paymentRequestDto);

        paymentRepository.getEntityManager().clear();

        PaymentEntity payment = paymentRepository.findById(paymentResponseDto.id());

        BigDecimal expectedTotalPrice = paymentRequestDto.price().multiply(BigDecimal.valueOf(paymentRequestDto.productQuantity())).setScale(2, RoundingMode.HALF_UP);

        BigDecimal actualBalance = payment.getPayer().getBalance().add(expectedTotalPrice).setScale(2, RoundingMode.HALF_UP);
        assertEquals(oldBalance, actualBalance, "Balance mismatch");
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus(), "Payment status mismatch");

        assertEquals(expectedTotalPrice, paymentResponseDto.totalPrice().setScale(2, RoundingMode.HALF_UP), "Total price between request and response mismatch");
        assertEquals(paymentRequestDto.price().setScale(2, RoundingMode.HALF_UP), paymentResponseDto.price().setScale(2, RoundingMode.HALF_UP), "Price between request and response mismatch");
        assertEquals(paymentRequestDto.productId(), paymentResponseDto.productId(), "Product ID between request and response mismatch");
        assertEquals(paymentRequestDto.productQuantity(), paymentResponseDto.productQuantity(), "Product quantity between request and response mismatch");

        assertEquals(expectedTotalPrice, payment.getTotalPrice().setScale(2, RoundingMode.HALF_UP), "Total price between request and entity mismatch");
        assertEquals(paymentRequestDto.price().setScale(2, RoundingMode.HALF_UP), payment.getPrice().setScale(2, RoundingMode.HALF_UP), "Price between request and entity mismatch");
        assertEquals(paymentRequestDto.productId(), payment.getProductId(), "Product ID between request and entity mismatch");
        assertEquals(paymentRequestDto.productQuantity(), payment.getProductQuantity(), "Product quantity between request and entity mismatch");
    }

    @Test
    public void testProcess_FailWalletMissing() {
        UUID userId = UUID.fromString("e1f3c2b4-5a7f-4b3d-9c1e-2f7a3d5b8c20");
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(BigDecimal.valueOf(100), UUID.randomUUID(), 10, userId);
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            PaymentResponseDto paymentResponseDto = paymentSagaService.process(paymentRequestDto);
        });

        assertEquals(404, exception.getResponse().getStatus());
        assertEquals("User's wallet not found", exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "N/A, e1f3c2b4-5a7f-4b3d-eeee-2f7a3d5b8c20, 10, e1f3c2b4-5a7f-4b3d-9c1e-2f7a3d5b8c20, Price omitted",
            "100, N/A, 10, e1f3c2b4-5a7f-4b3d-9c1e-2f7a3d5b8c20, Product id omitted",
            "100, e1f3c2b4-5a7f-4b3d-eeee-2f7a3d5b8c20, N/A, e1f3c2b4-5a7f-4b3d-9c1e-2f7a3d5b8c20, Product quantity omitted",
            "100, e1f3c2b4-5a7f-4b3d-eeee-2f7a3d5b8c20, 10, N/A, User id omitted",

            "N/A, N/A, 10, e1f3c2b4-5a7f-4b3d-9c1e-2f7a3d5b8c20, Price omitted;Product id omitted",
            "N/A, e1f3c2b4-5a7f-4b3d-eeee-2f7a3d5b8c20, N/A, e1f3c2b4-5a7f-4b3d-9c1e-2f7a3d5b8c20, Price omitted;Product quantity omitted",
            "N/A, e1f3c2b4-5a7f-4b3d-eeee-2f7a3d5b8c20, 10, N/A, Price omitted;User id omitted",
            "100, N/A, N/A, e1f3c2b4-5a7f-4b3d-9c1e-2f7a3d5b8c20, Product id omitted;Product quantity omitted",
            "100, N/A, 10, N/A, Product id omitted;User id omitted",
            "100, e1f3c2b4-5a7f-4b3d-eeee-2f7a3d5b8c20, N/A, N/A, Product quantity omitted;User id omitted",

            "N/A, N/A, N/A, e1f3c2b4-5a7f-4b3d-9c1e-2f7a3d5b8c20, Price omitted;Product id omitted;Product quantity omitted",
            "N/A, N/A, 10, N/A, Price omitted;Product id omitted;User id omitted",
            "N/A, e1f3c2b4-5a7f-4b3d-eeee-2f7a3d5b8c20, N/A, N/A, Price omitted;Product quantity omitted;User id omitted",
            "100, N/A, N/A, N/A, Product id omitted;Product quantity omitted;User id omitted",

            "N/A, N/A, N/A, N/A, Price omitted;Product id omitted;Product quantity omitted;User id omitted"
    }, nullValues = {"N/A"})
    public void testProcess_FailMissingDtoFields(BigDecimal price, UUID productId, Integer productQuantity, UUID userId, String messages) {
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(price, productId, productQuantity, userId);
        Set<ConstraintViolation<PaymentRequestDto>> violations = validator.validate(paymentRequestDto);

        String[] messagesArray = messages.split(";");
        List<String> violationMessages = violations.stream().map(ConstraintViolation::getMessage).toList();

        assertFalse(violations.isEmpty());

        for (String message : messagesArray) {
            assertTrue(violationMessages.contains(message));
        }
    }

    @Test
    public void testProcess_FailNegativePrice() {
        UUID userId = getUserId();
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(BigDecimal.valueOf(-100), UUID.randomUUID(), 10, userId);

        Set<ConstraintViolation<PaymentRequestDto>> violations = validator.validate(paymentRequestDto);

        assertEquals(1, violations.size());
        String message = violations.iterator().next().getMessage();
        assertEquals("Price cannot be negative", message);
    }

    @Test
    public void testProcess_FailQuantityLessThanOne() {
        UUID userId = getUserId();
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(BigDecimal.valueOf(100), UUID.randomUUID(), 0, userId);

        Set<ConstraintViolation<PaymentRequestDto>> violations = validator.validate(paymentRequestDto);

        assertEquals(1, violations.size());
        String message = violations.iterator().next().getMessage();
        assertEquals("Product quantity must be at least 1", message);
    }

    @Test
    public void testProcess_FailInsufficientBalance() {
        UUID userId = getUserId();
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(BigDecimal.valueOf(1000000), UUID.randomUUID(), 10, userId);
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            PaymentResponseDto paymentResponseDto = paymentSagaService.process(paymentRequestDto);
        });

        assertEquals(400, exception.getResponse().getStatus());
        assertEquals("Insufficient balance", exception.getMessage());
    }

    @Test
    public void testRefund_Success() {
        UUID paymentId = UUID.fromString("e3b2c1d4-7f5a-4c3b-8d1e-9b2c3a4f5d64");
        PaymentEntity before = paymentRepository.findById(paymentId);
        BigDecimal expectedBalance = before.getPayer().getBalance().setScale(2, RoundingMode.HALF_UP);

        paymentSagaService.refund(paymentId);

        paymentRepository.getEntityManager().clear();

        PaymentEntity payment = paymentRepository.findById(paymentId);
        assertNotNull(payment);

        BigDecimal actualBalance = payment.getPayer().getBalance().subtract(payment.getTotalPrice()).setScale(2, RoundingMode.HALF_UP);

        assertEquals(expectedBalance, actualBalance);
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
    }

    @Test
    public void testProcessSagaFlow() {
        // 1. Prepare the Mock Incoming Message
        InMemorySource<KafkaPaymentRequestDto> commitSource = connector.source("payment-service-commit");
        InMemorySink<KafkaPaymentResponseDto> responseSink = connector.sink("payment-service-response");

        UUID userId = getUserId();
        WalletEntity wallet = walletRepository.find("userId", userId).firstResult();
        BigDecimal oldBalance = wallet.getBalance();

        UUID productId = UUID.randomUUID();
        int productQuantity = 1;
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(BigDecimal.valueOf(100), productId, productQuantity, userId);

        KafkaPaymentRequestDto payload = new KafkaPaymentRequestDto(
                userId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                productId,
                productQuantity,
                paymentRequestDto
        );

        // 2. Send the message to the @Incoming channel
        commitSource.send(payload);

        // 3. Assertions: Database State
        await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> responseSink.received().size() == 1);

        // 4. Assertions: Outgoing Kafka Event
        assertEquals(1, responseSink.received().size(), "Should have sent 1 response message");

        KafkaPaymentResponseDto response = responseSink.received().getFirst().getPayload();

        BigDecimal expectedTotalPrice = paymentRequestDto.price().multiply(BigDecimal.valueOf(paymentRequestDto.productQuantity())).setScale(2, RoundingMode.HALF_UP);

        assertEquals(payload.sagaId(), response.sagaId(), "Saga ID must match for correlation");
        assertEquals(payload.userId(), response.userId(), "User ID must match for correlation");

        assertEquals(expectedTotalPrice, response.paymentResponseDto().totalPrice().setScale(2, RoundingMode.HALF_UP), "Total price between request and response mismatch");
        assertEquals(paymentRequestDto.price().setScale(2, RoundingMode.HALF_UP), response.paymentResponseDto().price().setScale(2, RoundingMode.HALF_UP), "Price between request and response mismatch");
        assertEquals(paymentRequestDto.productId(), response.paymentResponseDto().productId(), "Product ID between request and response mismatch");
        assertEquals(paymentRequestDto.productQuantity(), response.paymentResponseDto().productQuantity(), "Product quantity between request and response mismatch");

        paymentRepository.getEntityManager().clear();

        PaymentEntity payment = paymentRepository.findById(response.paymentResponseDto().id());

        BigDecimal actualBalance = payment.getPayer().getBalance().add(expectedTotalPrice).setScale(2, RoundingMode.HALF_UP);
        assertEquals(oldBalance, actualBalance, "Balance mismatch");

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus(), "Payment status mismatch");
        assertEquals(expectedTotalPrice, payment.getTotalPrice().setScale(2, RoundingMode.HALF_UP), "Total price between request and entity mismatch");
        assertEquals(paymentRequestDto.price().setScale(2, RoundingMode.HALF_UP), payment.getPrice().setScale(2, RoundingMode.HALF_UP), "Price between request and entity mismatch");
        assertEquals(paymentRequestDto.productId(), payment.getProductId(), "Product ID between request and entity mismatch");
        assertEquals(paymentRequestDto.productQuantity(), payment.getProductQuantity(), "Product quantity between request and entity mismatch");

        responseSink.clear();
    }

    @Test
    public void testRefundProductSagaFlow() {
        InMemorySource<KafkaPaymentErrorDto> rollbackSource = connector.source("payment-service-rollback");
        InMemorySink<KafkaPaymentErrorDto> errorSink = connector.sink("payment-service-error");

        UUID paymentId = UUID.fromString("e3b2c1d4-7f5a-4c3b-8d1e-9b2c3a4f5d64");
        PaymentEntity before = paymentRepository.findById(paymentId);
        BigDecimal expectedBalance = before.getPayer().getBalance().setScale(2, RoundingMode.HALF_UP);

        KafkaPaymentErrorDto errorPayload = new KafkaPaymentErrorDto(paymentId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), before.getProductQuantity(), new KafkaErrorDto("Product reservation failed", 400));

        rollbackSource.send(errorPayload);

        await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> errorSink.received().size() == 1);

        assertEquals(1, errorSink.received().size());

        paymentRepository.getEntityManager().clear();

        PaymentEntity payment = paymentRepository.findById(paymentId);
        assertNotNull(payment);

        BigDecimal actualBalance = payment.getPayer().getBalance().subtract(payment.getTotalPrice()).setScale(2, RoundingMode.HALF_UP);

        assertEquals(expectedBalance, actualBalance);
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());

        errorSink.clear();
    }

}
