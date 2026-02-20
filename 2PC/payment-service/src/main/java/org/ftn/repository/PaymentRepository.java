package org.ftn.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.ftn.entity.PaymentEntity;

import java.util.UUID;

@ApplicationScoped
public class PaymentRepository implements PanacheRepositoryBase<PaymentEntity, UUID> {
}
