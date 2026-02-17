package org.ftn.entity;

import jakarta.persistence.*;
import org.ftn.constant.OrderStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_entity")
@Access(AccessType.PROPERTY)
public class OrderEntity {
    private UUID id;
    private UUID productId;
    private int quantity;
    private UUID userId;
    private Instant createdAt;
    private OrderStatus status;
    private boolean locked;
    private UUID lockId;

    @Id
    @GeneratedValue
    public UUID getId() {
        return id;
    }

    @Column(name = "product_id")
    public UUID getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    @Column(name = "user_id")
    public UUID getUserId() {
        return userId;
    }

    @Column(name = "created_at")
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Enumerated(EnumType.STRING)
    public OrderStatus getStatus() {
        return status;
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

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public void setQuantity(int amount) {
        this.quantity = amount;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
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
