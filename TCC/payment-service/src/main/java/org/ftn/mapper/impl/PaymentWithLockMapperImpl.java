package org.ftn.mapper.impl;

import jakarta.enterprise.context.ApplicationScoped;
import org.ftn.constant.PaymentStatus;
import org.ftn.dto.PaymentRequestDto;
import org.ftn.dto.PaymentWithLockRequestDto;
import org.ftn.dto.PaymentWithLockResponseDto;
import org.ftn.entity.PaymentEntity;
import org.ftn.mapper.PaymentMapper;
import org.ftn.mapper.PaymentWithLockMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@ApplicationScoped
public class PaymentWithLockMapperImpl implements PaymentWithLockMapper {
    @Override
    public PaymentEntity toEntity(PaymentWithLockRequestDto dto) {
        PaymentEntity entity = new PaymentEntity();
        entity.setPrice(dto.price());
        entity.setProductId(dto.productId());
        entity.setProductQuantity(dto.productQuantity());
        entity.setStatus(PaymentStatus.PENDING);
        entity.setPayedAt(Instant.now());
        entity.setTotalPrice(entity.getPrice().multiply(new BigDecimal(entity.getProductQuantity())));
        entity.setLockId(dto.txId());
        return entity;
    }

    @Override
    public PaymentWithLockResponseDto toDto(PaymentEntity entity) {
        return new PaymentWithLockResponseDto(
                entity.getId(),
                entity.getPrice(),
                entity.getProductId(),
                entity.getProductQuantity(),
                LocalDateTime.ofInstant(entity.getPayedAt(), ZoneId.systemDefault()),
                entity.getStatus(),
                entity.getPayer().getUserId(),
                entity.getTotalPrice(),
                entity.isLocked(),
                entity.getLockId()
        );
    }
}
