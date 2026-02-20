package org.ftn.inventory.repository;

import io.quarkus.agroal.DataSource;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.ftn.inventory.entity.InventoryEntity;

import java.util.UUID;

@ApplicationScoped
@DataSource("inventory")
public class InventoryRepository implements PanacheRepositoryBase<InventoryEntity, UUID> {
}
