package org.ftn.dto;

import org.ftn.constant.CoordinatorTransactionState;

import java.util.UUID;

public record CheckoutDto(UUID id,
                          CoordinatorTransactionState state) {
}
