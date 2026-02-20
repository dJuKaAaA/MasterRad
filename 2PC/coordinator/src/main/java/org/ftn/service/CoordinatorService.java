package org.ftn.service;

import org.ftn.dto.CreateOrderRequestDto;

import java.util.UUID;

public interface CoordinatorService {
    void createOrder(CreateOrderRequestDto dto, UUID userId);
}
