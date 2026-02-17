package org.ftn.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.ftn.entity.ProductEntity;

import java.util.UUID;

@ApplicationScoped
public class ProductRepository implements PanacheRepositoryBase<ProductEntity, UUID> {
}
