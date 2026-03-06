package org.ftn.entity;

import jakarta.persistence.*;
import org.ftn.constant.NeedRollback;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "participant_data_entity")
@Access(AccessType.PROPERTY)
public class ParticipantDataEntity {
    private UUID id;
    private UUID paymentId;
    private UUID orderId;
    private UUID productId;
    private int amount;
    private BigDecimal price;
    private UUID userId;

    public ParticipantDataEntity() {
    }

    public ParticipantDataEntity(UUID id, UUID paymentId, UUID orderId, UUID productId, int amount, BigDecimal price) {
        this.id = id;
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.productId = productId;
        this.amount = amount;
        this.price = price;
    }

    @Id
    @GeneratedValue
    public UUID getId() {
        return id;
    }

    @Column(name = "payment_id")
    public UUID getPaymentId() {
        return paymentId;
    }

    @Column(name = "order_id")
    public UUID getOrderId() {
        return orderId;
    }

    @Column(name = "product_id")
    public UUID getProductId() {
        return productId;
    }

    public int getAmount() {
        return amount;
    }

    public BigDecimal getPrice() {
        return price;
    }

    @Column(name = "user_id")
    public UUID getUserId() {
        return userId;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}
