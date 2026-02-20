package org.ftn.mapper;

import org.ftn.dto.PaymentRequestDto;
import org.ftn.dto.PaymentResponseDto;
import org.ftn.entity.PaymentEntity;

public interface PaymentMapper {
    PaymentEntity toEntity(PaymentRequestDto dto);
    PaymentResponseDto toDto(PaymentEntity entity);
}
