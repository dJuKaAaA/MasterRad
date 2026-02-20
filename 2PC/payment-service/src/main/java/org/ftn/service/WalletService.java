package org.ftn.service;

import org.ftn.dto.WalletResponseDto;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletService {
    WalletResponseDto createForUser(UUID userId);
    WalletResponseDto getForUser(UUID userId);
    WalletResponseDto increaseBalanceForUser(UUID userId, BigDecimal balance);
}
