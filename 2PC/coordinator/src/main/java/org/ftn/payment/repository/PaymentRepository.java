package org.ftn.payment.repository;

import io.quarkus.agroal.DataSource;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.ftn.payment.entity.PaymentEntity;

import java.util.UUID;

@ApplicationScoped
@DataSource("payment")
public class PaymentRepository implements PanacheRepositoryBase<PaymentEntity, UUID> {
}
