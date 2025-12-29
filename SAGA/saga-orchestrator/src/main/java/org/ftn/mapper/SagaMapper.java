package org.ftn.mapper;

import org.ftn.dto.SagaResponseDto;
import org.ftn.entity.SagaEntity;

public interface SagaMapper {
    SagaResponseDto toDto(SagaEntity entity);
}
