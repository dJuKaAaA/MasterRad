package org.ftn.order.repository;

import io.quarkus.agroal.DataSource;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.ftn.order.entity.OrderEntity;

import java.util.UUID;

@ApplicationScoped
@DataSource("order")
public class OrderRepository implements PanacheRepositoryBase<OrderEntity, UUID> {
}
