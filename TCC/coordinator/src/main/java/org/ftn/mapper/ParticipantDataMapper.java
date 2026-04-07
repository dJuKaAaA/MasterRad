package org.ftn.mapper;

import org.ftn.dto.ParticipantDataDto;
import org.ftn.entity.ParticipantDataEntity;

public interface ParticipantDataMapper {
    ParticipantDataDto toDto(ParticipantDataEntity entity);
}
