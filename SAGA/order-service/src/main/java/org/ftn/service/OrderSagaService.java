package org.ftn.service;

import org.ftn.dto.OrderRequestDto;
import org.ftn.dto.OrderResponseDto;

import java.util.UUID;

public interface OrderSagaService {
    OrderResponseDto createOrder(OrderRequestDto dto);
    void cancelOrder(UUID id);
}
