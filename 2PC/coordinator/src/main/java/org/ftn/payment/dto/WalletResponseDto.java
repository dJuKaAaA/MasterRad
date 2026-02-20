package org.ftn.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletResponseDto(UUID id,
                                UUID userId,
                                BigDecimal balance) {
}
