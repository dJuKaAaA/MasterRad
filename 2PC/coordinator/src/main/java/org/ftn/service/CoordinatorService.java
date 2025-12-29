package org.ftn.service;

import org.ftn.dto.CreateOrderRequestDto;

import java.util.UUID;

public interface CoordinatorService {
void createTransaction(CreateOrderRequestDto createOrderRequestDto, UUID coordinatorId);
}
