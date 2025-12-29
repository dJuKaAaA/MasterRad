package org.ftn.service;

import org.ftn.dto.InventoryResponseDto;

import java.util.UUID;

public interface InventorySagaService {
    InventoryResponseDto reserve(UUID productId, int amount);
    void release(UUID productId, int amount);
}
