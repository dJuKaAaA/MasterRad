package org.ftn.service;

import org.ftn.dto.PaymentRequestDto;
import org.ftn.dto.PaymentResponseDto;

import java.util.UUID;

public interface PaymentSagaService {
    PaymentResponseDto process(PaymentRequestDto dto);
    void refund(UUID id);
}
