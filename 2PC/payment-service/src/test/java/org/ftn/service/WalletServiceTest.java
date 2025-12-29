package org.ftn.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.ftn.dto.WalletResponseDto;
import org.ftn.entity.WalletEntity;
import org.ftn.repository.WalletRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
public class WalletServiceTest {
    @Inject
    WalletService walletService;
    @Inject
    WalletRepository walletRepository;

    private UUID getDummyUserId() {
        return UUID.fromString("ffff7a2b-5add-4197-ad56-48be327ea13c");
    }

    private UUID getExistingUserId() {
        return UUID.fromString("daa45fd6-3500-4a0d-914d-052082303122");
    }

    @Transactional
    @Test
    public void testCreateForUser_Success() {
        final UUID userId = getDummyUserId();
        WalletResponseDto response = walletService.createForUser(userId);
        assertEquals(userId, response.userId());
        assertEquals(0, response.balance().compareTo(BigDecimal.ZERO));

        // Cleanup
        walletRepository.deleteById(response.id());
    }

    @Test
    public void testCreateForUser_AlreadyHasWallet() {
        final UUID userId = getExistingUserId();
        BadRequestException exception = assertThrows(BadRequestException.class, () -> walletService.createForUser(userId));
        assertEquals("User already has a wallet", exception.getMessage());
    }

    @Test
    public void testGetForUser_Success() {
        final UUID userId = getExistingUserId();
        WalletResponseDto response = walletService.getForUser(userId);
        assertEquals(userId, response.userId());
    }

    @Test
    public void testGetForUser_NotFound() {
        final UUID userId = getDummyUserId();
        NotFoundException exception = assertThrows(NotFoundException.class, () -> walletService.getForUser(userId));
        assertEquals("Wallet not found", exception.getMessage());
    }

    @Transactional
    @Test
    public void testIncreaseBalanceForUser_Success() {
        UUID userId = getDummyUserId();
        WalletEntity wallet = new WalletEntity();
        wallet.setUserId(userId);
        wallet.setBalance(BigDecimal.valueOf(1_000_000));
        walletRepository.persist(wallet);

        WalletResponseDto response = walletService.increaseBalanceForUser(userId, BigDecimal.valueOf(100));
        assertEquals(userId, response.userId());
        assertEquals(0, response.balance().compareTo(BigDecimal.valueOf(1_000_100)));

        // Cleanup
        walletRepository.delete(wallet);
    }

    @Test
    public void testIncreaseBalanceForUser_NotFound() {
        UUID userId = getDummyUserId();
        NotFoundException exception = assertThrows(NotFoundException.class, () -> walletService.increaseBalanceForUser(userId, BigDecimal.TEN));
        assertEquals("Wallet not found", exception.getMessage());
    }

    @Transactional
    @Test
    public void testIncreaseBalanceForUser_BadBalance() {
        UUID userId = getDummyUserId();
        WalletEntity wallet = new WalletEntity();
        wallet.setUserId(userId);
        wallet.setBalance(BigDecimal.valueOf(1_000_000));
        walletRepository.persist(wallet);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> walletService.increaseBalanceForUser(userId, BigDecimal.ZERO));
        assertEquals("Balance must be at least 1", exception.getMessage());

        // Cleanup
        walletRepository.delete(wallet);
    }
}
