package org.ftn.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.ftn.entity.InventoryEntity;

import java.util.UUID;

@ApplicationScoped
public class InventoryRepository implements PanacheRepositoryBase<InventoryEntity, UUID> {
}
