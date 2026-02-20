package org.ftn.service;

import org.ftn.dto.PaymentRequestDto;
import org.ftn.dto.PaymentResponseDto;

import java.util.UUID;

public interface PaymentTCCService {
    PaymentResponseDto tccTry(PaymentRequestDto dto);
    void tccCommit(UUID id);
    void tccCancel(UUID id);
}
