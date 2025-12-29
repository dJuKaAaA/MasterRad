package org.ftn.mapper.impl;

import jakarta.enterprise.context.ApplicationScoped;
import org.ftn.dto.SagaResponseDto;
import org.ftn.entity.SagaEntity;
import org.ftn.mapper.SagaMapper;

import java.time.LocalDateTime;
import java.time.ZoneId;

@ApplicationScoped
public class SagaMapperImpl implements SagaMapper {

    @Override
    public SagaResponseDto toDto(SagaEntity entity) {
        return new SagaResponseDto(
                entity.getId().toString(),
                entity.getState().name(),
                LocalDateTime.ofInstant(entity.getCreatedAt(), ZoneId.systemDefault()),
                LocalDateTime.ofInstant(entity.getLastUpdated(), ZoneId.systemDefault()),
                entity.getFailureReason(),
                entity.getTransactionState().name(),
                entity.getIdempotencyKey().toString()
        );
    }
}
