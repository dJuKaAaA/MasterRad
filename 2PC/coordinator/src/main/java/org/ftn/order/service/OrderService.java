package org.ftn.order.service;

import org.ftn.order.dto.OrderRequestDto;
import org.ftn.order.dto.OrderResponseDto;

import java.util.UUID;

public interface OrderService {
    OrderResponseDto createOrder(OrderRequestDto dto);
    void cancelOrder(UUID id);
}
