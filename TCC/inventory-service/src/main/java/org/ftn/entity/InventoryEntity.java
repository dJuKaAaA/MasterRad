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
    private Integer reservedAmount;

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

    @Column(name = "reserved_amount")
    public Integer getReservedAmount() {
        return reservedAmount;
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
        this.availableStock -= Math.abs(amount);
    }

    public void increaseAvailableStock(int amount) {
        this.availableStock += Math.abs(amount);
    }

    public void increaseReservedStock(int amount) {
        this.reservedAmount += Math.abs(amount);
    }

    public void decreaseReservedStock(int amount) {
        this.reservedAmount -= Math.abs(amount);
    }

    public void setReservedAmount(Integer reservedAmount) {
        this.reservedAmount = reservedAmount;
    }
}
