package org.ftn.mapper;

import org.ftn.dto.CoordinatorTransactionDto;
import org.ftn.entity.CoordinatorTransactionEntity;

public interface CoordinatorTransactionMapper {
    CoordinatorTransactionDto toDto(CoordinatorTransactionEntity entity);
}
