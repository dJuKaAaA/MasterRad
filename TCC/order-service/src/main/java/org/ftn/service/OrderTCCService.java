package org.ftn.service;

import org.ftn.dto.OrderRequestDto;
import org.ftn.dto.OrderResponseDto;

import java.util.UUID;

public interface OrderTCCService {
    OrderResponseDto tccTry(OrderRequestDto dto);
    void tccCommit(UUID id);
    void tccCancel(UUID id);
}
