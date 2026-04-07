package org.ftn.mapper.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ftn.dto.CoordinatorTransactionDto;
import org.ftn.entity.CoordinatorTransactionEntity;
import org.ftn.mapper.CoordinatorTransactionMapper;
import org.ftn.mapper.ParticipantDataMapper;

@ApplicationScoped
public class CoordinatorTransactionMapperImpl implements CoordinatorTransactionMapper {
    private final ParticipantDataMapper participantDataMapper;

    @Inject
    public CoordinatorTransactionMapperImpl(ParticipantDataMapper participantDataMapper) {
        this.participantDataMapper = participantDataMapper;
    }

    @Override
    public CoordinatorTransactionDto toDto(CoordinatorTransactionEntity entity) {
        return new CoordinatorTransactionDto(
                entity.getId(),
                entity.getState(),
                entity.getDecision(),
                entity.getCreatedAt(),
                entity.getAbortReason(),
                entity.isCompleted(),
                participantDataMapper.toDto(entity.getParticipantData())
        );
    }
}
