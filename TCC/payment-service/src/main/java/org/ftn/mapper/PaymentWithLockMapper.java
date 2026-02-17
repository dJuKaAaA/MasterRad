package org.ftn.mapper;

import org.ftn.dto.PaymentRequestDto;
import org.ftn.dto.PaymentWithLockRequestDto;
import org.ftn.dto.PaymentWithLockResponseDto;
import org.ftn.entity.PaymentEntity;

public interface PaymentWithLockMapper {
    PaymentEntity toEntity(PaymentWithLockRequestDto dto);
    PaymentWithLockResponseDto toDto(PaymentEntity entity);
}
