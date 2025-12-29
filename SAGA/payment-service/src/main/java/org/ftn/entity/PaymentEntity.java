package org.ftn.entity;

import jakarta.persistence.*;
import org.ftn.constant.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_entity")
@Access(AccessType.PROPERTY)
public class PaymentEntity {
    private UUID id;
    private BigDecimal price;
    private UUID productId;
    private int productQuantity;
    private Instant payedAt;
    private PaymentStatus status;
    private WalletEntity payer;
    private BigDecimal totalPrice;

    @Id
    @GeneratedValue
    public UUID getId() {
        return id;
    }

    public BigDecimal getPrice() {
        return price;
    }

    @Column(name = "product_id")
    public UUID getProductId() {
        return productId;
    }

    @Column(name = "product_quantity")
    public int getProductQuantity() {
        return productQuantity;
    }

    @Column(name = "payed_at")
    public Instant getPayedAt() {
        return payedAt;
    }

    @Enumerated(EnumType.STRING)
    public PaymentStatus getStatus() {
        return status;
    }

    @ManyToOne(cascade = {CascadeType.MERGE})
    public WalletEntity getPayer() {
        return payer;
    }

    @Column(name = "total_price")
    public BigDecimal getTotalPrice() {
        return this.totalPrice;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public void setProductQuantity(int productQuantity) {
        this.productQuantity = productQuantity;
    }

    public void setPayedAt(Instant payedAt) {
        this.payedAt = payedAt;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public void setPayer(WalletEntity payer) {
        this.payer = payer;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
}
