package org.ftn.inventory.service;

import org.ftn.inventory.dto.InventoryResponseDto;

import java.util.UUID;

public interface InventoryService {
    InventoryResponseDto reserve(UUID productId, int amount);
    void release(UUID productId, int amount);
}
