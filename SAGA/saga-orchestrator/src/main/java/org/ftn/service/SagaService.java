package org.ftn.service;

import org.ftn.constant.SagaState;
import org.ftn.dto.CreateOrderRequestDto;
import org.ftn.dto.SagaResponseDto;
import org.ftn.entity.SagaEntity;

import java.util.UUID;

public interface SagaService {
    void createOrderTransaction(SagaEntity saga, CreateOrderRequestDto createOrderRequestDto, UUID userId);
    SagaResponseDto createOrderTransactionAsync(UUID idempotencyKey, CreateOrderRequestDto createOrderRequestDto, UUID userId);
    SagaState getState(UUID id);
}
