package org.ftn.service;

import org.ftn.dto.*;

import java.util.UUID;

public interface InventoryService {
    PageResponse<InventoryResponseDto> getAll(UUID merchantId, int page, int size);
    PageResponse<InventoryResponseDto> getAll(int page, int size);
    InventoryResponseDto get(UUID id);
    InventoryResponseDto get(UUID id, UUID merchantId);
    InventoryResponseDto create(InventoryRequestDto dto);
    InventoryResponseDto create(InventoryRequestDto dto, UUID merchantIdFromJwt);
    void delete(UUID id);
    ProductResponseDto discontinueProduct(UUID productId);
    InventoryResponseDto replenishStock(UUID id, int amount);
    ProductResponseDto updateProduct(UUID productId, ProductRequestDto productRequestDto);
}
