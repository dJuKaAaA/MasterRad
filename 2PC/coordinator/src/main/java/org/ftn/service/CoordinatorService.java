package org.ftn.service;

import org.ftn.constant.CoordinatorTransactionState;
import org.ftn.dto.CheckoutDto;
import org.ftn.dto.CreateOrderRequestDto;

import java.util.UUID;

public interface CoordinatorService {
    void createOrder(CreateOrderRequestDto dto, UUID userId);
    UUID update(UUID txId, CoordinatorTransactionState state);
    CheckoutDto get(UUID txId);
    CoordinatorTransactionState getState(UUID txId);
}
