package org.ftn.service;

import org.ftn.dto.PageResponse;
import org.ftn.dto.PaymentResponseDto;

import java.util.UUID;

public interface PaymentService {
    PageResponse<PaymentResponseDto> getAll(UUID userId, int page, int size);
    PageResponse<PaymentResponseDto> getAll(int page, int size);
    PaymentResponseDto get(UUID id);
    PaymentResponseDto get(UUID id, UUID userId);
}
