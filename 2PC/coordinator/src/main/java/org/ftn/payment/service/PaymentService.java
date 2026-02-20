package org.ftn.payment.service;

import org.ftn.payment.dto.PaymentRequestDto;
import org.ftn.payment.dto.PaymentResponseDto;

import java.util.UUID;

public interface PaymentService {
    PaymentResponseDto process(PaymentRequestDto dto);
    void refund(UUID id);
}
