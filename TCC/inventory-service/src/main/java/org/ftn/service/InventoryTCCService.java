package org.ftn.service;

import org.ftn.dto.InventoryResponseDto;

import java.util.UUID;

public interface InventoryTCCService {
    InventoryResponseDto tccTry(UUID productId, int amount);
    void tccCommit(UUID productId, int amount);
    void tccCancel(UUID productId, int amount);
}
