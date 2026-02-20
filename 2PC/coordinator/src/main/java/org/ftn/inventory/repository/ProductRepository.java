package org.ftn.inventory.repository;

import io.quarkus.agroal.DataSource;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.ftn.inventory.entity.ProductEntity;

import java.util.UUID;

@ApplicationScoped
@DataSource("inventory")
public class ProductRepository implements PanacheRepositoryBase<ProductEntity, UUID> {
}
