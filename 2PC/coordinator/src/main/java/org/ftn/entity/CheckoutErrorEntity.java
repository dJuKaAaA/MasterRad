package org.ftn.entity;

import jakarta.persistence.*;
import org.hibernate.validator.cfg.defs.UUIDDef;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "checkout_error_entity")
@Access(AccessType.PROPERTY)
public class CheckoutErrorEntity {
    private UUID id;
    private String message;
    private int status;
    private Instant timestamp;
    private UUID productId;
    private int amount;
    private UUID userId;

    @Id
    @GeneratedValue
    public UUID getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Column(name = "product_id")
    public UUID getProductId() {
        return productId;
    }

    public int getAmount() {
        return amount;
    }

    @Column(name = "user_id")
    public UUID getUserId() {
        return userId;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}
