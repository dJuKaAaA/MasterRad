package org.ftn.repository;

import org.ftn.entity.PaymentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringPaymentRepository extends JpaRepository<PaymentEntity, UUID> {
    Page<PaymentEntity> findAllByPayerUserId(UUID userId, Pageable pageable);
}
