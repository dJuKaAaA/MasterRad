package org.ftn.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "participant_data_entity")
@Access(AccessType.PROPERTY)
public class RecoveryDataEntity {
    private UUID id;
    private UUID paymentId;
    private UUID orderId;
    private UUID productId;
    private int amount;
    private BigDecimal price;
    private UUID userId;
    private String errorMessage;
    private Integer errorStatus;

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

    @Column(name = "error_message")
    public String getErrorMessage() {
        return errorMessage;
    }

    @Column(name = "error_status")
    public Integer getErrorStatus() {
        return errorStatus;
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

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setErrorStatus(Integer errorStatus) {
        this.errorStatus = errorStatus;
    }
}
