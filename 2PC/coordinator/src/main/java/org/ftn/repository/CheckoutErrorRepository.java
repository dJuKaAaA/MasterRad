package org.ftn.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.ftn.entity.CheckoutErrorEntity;

import java.util.UUID;

@ApplicationScoped
public class CheckoutErrorRepository implements PanacheRepositoryBase<CheckoutErrorEntity, UUID> {
}
