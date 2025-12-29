package org.ftn.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.ftn.entity.SagaEntity;

import java.util.UUID;

@ApplicationScoped
public class SagaRepository implements PanacheRepositoryBase<SagaEntity, UUID> {
}
