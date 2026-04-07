package org.ftn.mapper.impl;

import jakarta.enterprise.context.ApplicationScoped;
import org.ftn.dto.ParticipantDataDto;
import org.ftn.entity.ParticipantDataEntity;
import org.ftn.mapper.ParticipantDataMapper;

@ApplicationScoped
public class ParticipantDataMapperImpl implements ParticipantDataMapper {
    @Override
    public ParticipantDataDto toDto(ParticipantDataEntity entity) {
        return new ParticipantDataDto(
                entity.getId(),
                entity.getPaymentId(),
                entity.getOrderId(),
                entity.getProductId(),
                entity.getAmount(),
                entity.getPrice(),
                entity.getUserId()
        );
    }
}
