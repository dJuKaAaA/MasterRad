package org.ftn.service;

import org.ftn.constant.OrderStatus;
import org.ftn.dto.OrderResponseDto;
import org.ftn.dto.PageResponse;

import java.util.UUID;

public interface OrderService {
    PageResponse<OrderResponseDto> getAll(int page, int size);
    PageResponse<OrderResponseDto> getAll(int page, int size, OrderStatus status);
    OrderResponseDto get(UUID id);
    OrderResponseDto get(UUID id, UUID userId);
    PageResponse<OrderResponseDto> getAllByUserId(UUID userId, int page, int size);
    PageResponse<OrderResponseDto> getAllByProductId(UUID productId, int page, int size);
}
