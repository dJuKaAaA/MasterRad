package org.ftn.service;

import org.ftn.dto.InventoryWithLockResponseDto;
import org.ftn.dto.VoteResponse;

import java.util.UUID;

public interface Inventory2PCService {
    VoteResponse prepare(UUID productId, int amount, UUID txId);
    void commit(UUID productId, int amount, UUID lockId);
    void rollback(UUID productId, int amount, UUID lockId);
}
