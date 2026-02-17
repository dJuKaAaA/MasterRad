package org.ftn.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.ftn.entity.CoordinatorTransactionEntity;

import java.util.UUID;

@ApplicationScoped
public class CoordinatorTransactionRepository implements PanacheRepositoryBase<CoordinatorTransactionEntity, UUID> {
}
