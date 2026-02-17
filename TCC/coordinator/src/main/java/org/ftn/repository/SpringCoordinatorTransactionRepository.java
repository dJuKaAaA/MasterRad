package org.ftn.repository;

import org.ftn.constant.CoordinatorTransactionState;
import org.ftn.entity.CoordinatorTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;

public interface SpringCoordinatorTransactionRepository extends JpaRepository<CoordinatorTransactionEntity, UUID> {
    Collection<CoordinatorTransactionEntity> findAllByStateIn(Collection<CoordinatorTransactionState> states);
}
