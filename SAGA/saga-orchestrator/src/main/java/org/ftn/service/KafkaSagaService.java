package org.ftn.service;

import org.ftn.dto.CreateOrderRequestDto;
import org.ftn.dto.SagaResponseDto;

import java.util.UUID;

public interface KafkaSagaService {
    SagaResponseDto createOrderTransactionKafka(UUID idempotencyKey, CreateOrderRequestDto createOrderRequestDto, UUID userId);
}
