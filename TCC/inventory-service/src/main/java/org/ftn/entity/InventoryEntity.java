package org.ftn.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_entity")
@Access(AccessType.PROPERTY)
public class InventoryEntity {
    private UUID id;
    private ProductEntity product;
    private int availableStock;
    private Instant createdAt;
    private Instant lastUpdatedAt;
    private boolean locked;
    private UUID lockId;

    @Id
    @GeneratedValue
    public UUID getId() {
        return id;
    }

    @OneToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    public ProductEntity getProduct() {
        return product;
    }

    @Column(name = "available_stock")
    public int getAvailableStock() {
        return availableStock;
    }

    @Column(name = "created_at")
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Column(name = "last_updated_at")
    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public boolean isLocked() {
        return locked;
    }

    @Column(name = "lock_id")
    public UUID getLockId() {
        return lockId;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setProduct(ProductEntity product) {
        this.product = product;
    }

    public void setAvailableStock(int availableStock) {
        this.availableStock = availableStock;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public void decreaseAvailableStock(int amount) {
        this.availableStock -= amount;
    }

    public void increaseAvailableStock(int amount) {
        this.availableStock += amount;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public void setLockId(UUID lockId) {
        this.lockId = lockId;
    }

    public boolean tryUnlock(UUID lockId) {
        if (lockId.equals(this.lockId)) {
            this.locked = false;
            this.lockId = null;
            return true;
        }
        return false;
    }
}
