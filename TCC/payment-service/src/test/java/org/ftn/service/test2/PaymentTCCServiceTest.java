package org.ftn.service.test2;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import org.ftn.constant.PaymentStatus;
import org.ftn.dto.PaymentRequestDto;
import org.ftn.dto.PaymentResponseDto;
import org.ftn.entity.PaymentEntity;
import org.ftn.entity.WalletEntity;
import org.ftn.repository.PaymentRepository;
import org.ftn.repository.WalletRepository;
import org.ftn.service.PaymentTCCService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class PaymentTCCServiceTest {
    @Inject
    PaymentTCCService paymentTCCService;
    @Inject
    PaymentRepository paymentRepository;
    @Inject
    WalletRepository walletRepository;
    @Inject
    Validator validator;

    private UUID getUserId() {
        return UUID.fromString("19e5ddb1-4c66-4d17-ad06-e8a6af23ed58");
    }

    @Test
    public void testTCCTry_Success() {
        UUID userId = getUserId();

        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(BigDecimal.valueOf(100), UUID.randomUUID(), 10, userId);

        PaymentResponseDto paymentResponseDto = paymentTCCService.tccTry(paymentRequestDto);

        paymentRepository.getEntityManager().clear();

        PaymentEntity payment = paymentRepository.findById(paymentResponseDto.id());

        BigDecimal expectedTotalPrice = paymentRequestDto.price().multiply(BigDecimal.valueOf(paymentRequestDto.productQuantity())).setScale(2, RoundingMode.HALF_UP);

        assertEquals(PaymentStatus.PENDING, payment.getStatus(), "Payment status mismatch");

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
    public void testTCCTry_FailWalletMissing() {
        UUID userId = UUID.fromString("e1f3c2b4-5a7f-4b3d-9c1e-2f7a3d5b8c20");
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(BigDecimal.valueOf(100), UUID.randomUUID(), 10, userId);
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            PaymentResponseDto paymentResponseDto = paymentTCCService.tccTry(paymentRequestDto);
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
    public void testTCCTry_FailMissingDtoFields(BigDecimal price, UUID productId, Integer productQuantity, UUID userId, String messages) {
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
    public void testTCCTry_FailNegativePrice() {
        UUID userId = getUserId();
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(BigDecimal.valueOf(-100), UUID.randomUUID(), 10, userId);

        Set<ConstraintViolation<PaymentRequestDto>> violations = validator.validate(paymentRequestDto);

        assertEquals(1, violations.size());
        String message = violations.iterator().next().getMessage();
        assertEquals("Price cannot be negative", message);
    }

    @Test
    public void testTCCTry_FailQuantityLessThanOne() {
        UUID userId = getUserId();
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(BigDecimal.valueOf(100), UUID.randomUUID(), 0, userId);

        Set<ConstraintViolation<PaymentRequestDto>> violations = validator.validate(paymentRequestDto);

        assertEquals(1, violations.size());
        String message = violations.iterator().next().getMessage();
        assertEquals("Product quantity must be at least 1", message);
    }

    @Test
    public void testTCCTry_FailInsufficientBalance() {
        UUID userId = getUserId();
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(BigDecimal.valueOf(1000000), UUID.randomUUID(), 10, userId);
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            PaymentResponseDto paymentResponseDto = paymentTCCService.tccTry(paymentRequestDto);
        });

        assertEquals(400, exception.getResponse().getStatus());
        assertEquals("Insufficient balance", exception.getMessage());
    }

    @Test
    public void testTCCCommit_Success() {
        // Setup
        UUID userId = getUserId();
        WalletEntity wallet = walletRepository.find("userId", userId).firstResult();
        BigDecimal oldBalance = wallet.getBalance();

        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(BigDecimal.valueOf(100), UUID.randomUUID(), 10, userId);

        PaymentResponseDto paymentResponseDto = paymentTCCService.tccTry(paymentRequestDto);

        paymentTCCService.tccCommit(paymentResponseDto.id());

        paymentRepository.getEntityManager().clear();

        PaymentEntity payment = paymentRepository.findById(paymentResponseDto.id());
        assertNotNull(payment);
        BigDecimal actualBalance = payment.getPayer().getBalance().add(paymentResponseDto.totalPrice()).setScale(2, RoundingMode.HALF_UP);

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(oldBalance, actualBalance, "Balance mismatch");
    }

    @Test
    public void testTCCCommit_FailPaymentNotFound() {
        UUID paymentId = UUID.randomUUID();
        ServerErrorException exception = assertThrows(ServerErrorException.class, () -> paymentTCCService.tccCommit(paymentId));

        assertEquals("Payment not found while attempting commit", exception.getMessage());
        assertEquals(500, exception.getResponse().getStatus());
    }

    @Test
    public void testTCCCancel_Success() {
        // Setup
        UUID userId = getUserId();
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(BigDecimal.valueOf(100), UUID.randomUUID(), 10, userId);
        PaymentResponseDto paymentResponseDto = paymentTCCService.tccTry(paymentRequestDto);

        paymentTCCService.tccCancel(paymentResponseDto.id());

        paymentRepository.getEntityManager().clear();

        PaymentEntity payment = paymentRepository.findById(paymentResponseDto.id());
        assertNotNull(payment);

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
    }
}
