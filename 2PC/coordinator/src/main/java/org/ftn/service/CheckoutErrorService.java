package org.ftn.service;

import org.ftn.dto.CheckoutErrorDto;
import org.ftn.dto.CreateOrderRequestDto;

import java.util.UUID;

public interface CheckoutErrorService {
    void save(CreateOrderRequestDto requestDto, UUID userId, String message, int status);
}
