package org.ftn.util;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.ftn.constant.ProductStatus;
import org.ftn.entity.InventoryEntity;
import org.ftn.entity.ProductEntity;
import org.ftn.repository.InventoryRepository;
import org.ftn.repository.ProductRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class InventoryDataSeeder {
    @Inject
    EntityManager em;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void seed() {
        execute("DELETE FROM inventory_entity");
        execute("DELETE FROM product_entity");
        em.clear();

        execute("INSERT INTO product_entity (id, name, description, price, added_at, status, merchant_id) VALUES ('d572df76-b527-4e31-8aa3-9aa954d17100', 'Gaming Mouse', 'High precision RGB gaming mouse', 59.99, '2024-05-15T16:22:00Z', 'ACTIVE', '7e865ca7-a38e-4002-9569-fa6d01e9bdbf')");
        execute("INSERT INTO inventory_entity (id, product_id, available_stock, created_at, last_updated_at) VALUES ('a3c1d2f5-4e6b-4b3f-9a2c-1e7d3f6b8c10', 'd572df76-b527-4e31-8aa3-9aa954d17100', 100, '2024-05-15T16:22:00Z', '2024-05-15T16:22:00Z')");

        execute("INSERT INTO product_entity (id, name, description, price, added_at, status, merchant_id) VALUES ('d572df76-b527-4e31-8aa3-9aa954d17101', 'Gaming Keyboard', 'High precision RGB gaming keyboard', 129.99, '2024-05-16T07:18:00Z', 'ACTIVE', '7e865ca7-a38e-4002-9569-fa6d01e9bdbf')");
        execute("INSERT INTO inventory_entity (id, product_id, available_stock, created_at, last_updated_at) VALUES ('a3c1d2f5-4e6b-4b3f-9a2c-1e7d3f6b8c11', 'd572df76-b527-4e31-8aa3-9aa954d17101', 150, '2024-05-16T07:18:00Z', '2024-05-16T07:18:00Z')");

        execute("INSERT INTO product_entity (id, name, description, price, added_at, status, merchant_id) VALUES ('d572df76-b527-4e31-8aa3-9aa954d17102', 'XBOX Controller for PC', 'XBOX Controller built for PC use', 89.99, '2024-05-17T10:41:00Z', 'ACTIVE', '7e865ca7-a38e-4002-9569-fa6d01e9bdbf')");
        execute("INSERT INTO inventory_entity (id, product_id, available_stock, created_at, last_updated_at) VALUES ('a3c1d2f5-4e6b-4b3f-9a2c-1e7d3f6b8c12', 'd572df76-b527-4e31-8aa3-9aa954d17102', 200, '2024-05-17T10:41:00Z', '2024-05-17T10:41:00Z')");

        execute("INSERT INTO product_entity (id, name, description, price, added_at, status, merchant_id) VALUES ('d572df76-b527-4e31-8aa3-9aa954d17103', 'Gaming Monitor', 'Gaming Monitor with 300hz refresh rate with 4k resolution', 599.99, '2024-05-20T17:27:00Z', 'ACTIVE', '7e865ca7-a38e-4002-9569-fa6d01e9bdbf')");
        execute("INSERT INTO inventory_entity (id, product_id, available_stock, created_at, last_updated_at) VALUES ('a3c1d2f5-4e6b-4b3f-9a2c-1e7d3f6b8c13', 'd572df76-b527-4e31-8aa3-9aa954d17103', 20, '2024-05-20T17:27:00Z', '2024-05-20T17:27:00Z')");

        execute("INSERT INTO product_entity (id, name, description, price, added_at, status, merchant_id) VALUES ('d572df76-b527-4e31-8aa3-9aa954d17104', 'Gaming PC', 'PC with Ryzen 7 CPU, RTX 3060 6GB graphics card, 32GB ram and 1TB of SSD', 2099.99, '2024-06-01T06:21:00Z', 'ACTIVE', '7e865ca7-a38e-4002-9569-fa6d01e9bdbf')");
        execute("INSERT INTO inventory_entity (id, product_id, available_stock, created_at, last_updated_at) VALUES ('a3c1d2f5-4e6b-4b3f-9a2c-1e7d3f6b8c14', 'd572df76-b527-4e31-8aa3-9aa954d17104', 25, '2024-06-01T06:21:00Z', '2024-06-01T06:21:00Z')");

        execute("INSERT INTO product_entity (id, name, description, price, added_at, status, merchant_id) VALUES ('d572df76-b527-4e31-8aa3-9aa954d17105', 'RAM 32gb', '32gb of RAM', 999.99, '2025-11-29T16:21:00Z', 'DISCONTINUED', '7e865ca7-a38e-4002-9569-fa6d01e9bdbf')");
        execute("INSERT INTO inventory_entity (id, product_id, available_stock, created_at, last_updated_at) VALUES ('a3c1d2f5-4e6b-4b3f-9a2c-1e7d3f6b8c15', 'd572df76-b527-4e31-8aa3-9aa954d17105', 25, '2025-11-29T16:21:00Z', '2025-11-29T16:21:00Z')");
    }

    private void execute(String sql) {
        em.createNativeQuery(sql).executeUpdate();
    }

}