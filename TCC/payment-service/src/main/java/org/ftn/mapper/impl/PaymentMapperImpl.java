package org.ftn.mapper.impl;

import jakarta.enterprise.context.ApplicationScoped;
import org.ftn.constant.PaymentStatus;
import org.ftn.dto.PaymentRequestDto;
import org.ftn.dto.PaymentResponseDto;
import org.ftn.entity.PaymentEntity;
import org.ftn.mapper.PaymentMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@ApplicationScoped
public class PaymentMapperImpl implements PaymentMapper {
    @Override
    public PaymentEntity toEntity(PaymentRequestDto dto) {
        PaymentEntity entity = new PaymentEntity();
        entity.setPrice(dto.price());
        entity.setProductId(dto.productId());
        entity.setProductQuantity(dto.productQuantity());
        entity.setStatus(PaymentStatus.PENDING);
        entity.setTotalPrice(entity.getPrice().multiply(new BigDecimal(entity.getProductQuantity())));
        return entity;
    }

    @Override
    public PaymentResponseDto toDto(PaymentEntity entity) {
        return new PaymentResponseDto(
                entity.getId(),
                entity.getPrice(),
                entity.getProductId(),
                entity.getProductQuantity(),
                LocalDateTime.ofInstant(entity.getPayedAt(), ZoneId.systemDefault()),
                entity.getStatus(),
                entity.getPayer().getUserId(),
                entity.getTotalPrice()
        );
    }
}
