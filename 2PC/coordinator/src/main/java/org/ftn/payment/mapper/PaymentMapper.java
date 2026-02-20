package org.ftn.payment.mapper;

import org.ftn.payment.dto.PaymentRequestDto;
import org.ftn.payment.dto.PaymentResponseDto;
import org.ftn.payment.entity.PaymentEntity;

public interface PaymentMapper {
    PaymentEntity toEntity(PaymentRequestDto dto);
    PaymentResponseDto toDto(PaymentEntity entity);
}
