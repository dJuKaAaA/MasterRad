package org.ftn.service;

import org.ftn.dto.PaymentWithLockRequestDto;
import org.ftn.dto.VoteResponse;

import java.util.UUID;

public interface PaymentTCCService {
    VoteResponse prepare(PaymentWithLockRequestDto dto);
    void commit(UUID id, UUID lockId);
    void rollback(UUID id, UUID lockId);
}
