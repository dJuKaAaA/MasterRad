package org.ftn.service;

import org.ftn.dto.PaymentRequestDto;
import org.ftn.dto.PaymentWithLockRequestDto;
import org.ftn.dto.PaymentWithLockResponseDto;
import org.ftn.dto.VoteResponse;

import java.util.UUID;

public interface Payment2PCService {
    VoteResponse prepare(PaymentWithLockRequestDto dto);
    void commit(UUID id, UUID lockId);
    void rollback(UUID id, UUID lockId);
}
