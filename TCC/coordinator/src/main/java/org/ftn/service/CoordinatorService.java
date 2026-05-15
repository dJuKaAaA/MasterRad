package org.ftn.service;

import org.ftn.constant.CoordinatorTransactionState;
import org.ftn.dto.CoordinatorTransactionDto;
import org.ftn.dto.CreateOrderRequestDto;

import java.util.UUID;

public interface CoordinatorService {
    CoordinatorTransactionDto createTransaction(CreateOrderRequestDto createOrderRequestDto, UUID userId);
    CoordinatorTransactionDto getTransaction(UUID id);
    CoordinatorTransactionState getState(UUID id);
}
