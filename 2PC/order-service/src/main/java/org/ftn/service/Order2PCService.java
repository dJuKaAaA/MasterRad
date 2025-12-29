package org.ftn.service;

import org.ftn.dto.OrderRequestDto;
import org.ftn.dto.OrderWithLockRequestDto;
import org.ftn.dto.OrderWithLockResponseDto;
import org.ftn.dto.VoteResponse;

import java.util.UUID;

public interface Order2PCService {
    VoteResponse prepare(OrderWithLockRequestDto dto);
    void commit(UUID id, UUID lockId);
    void rollback(UUID id, UUID lockId);
}
