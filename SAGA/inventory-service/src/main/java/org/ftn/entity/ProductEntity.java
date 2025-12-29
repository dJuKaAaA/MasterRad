package org.ftn.entity;

import jakarta.persistence.*;
import org.ftn.constant.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_entity")
@Access(AccessType.PROPERTY)
public class ProductEntity {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private Instant addedAt;
    private ProductStatus status;
    private UUID merchantId;

    @Id
    @GeneratedValue
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    @Column(name = "added_at")
    public Instant getAddedAt() {
        return addedAt;
    }

    @Enumerated(EnumType.STRING)
    public ProductStatus getStatus() {
        return status;
    }

    @Column(name = "merchant_id")
    public UUID getMerchantId() {
        return merchantId;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setAddedAt(Instant addedAt) {
        this.addedAt = addedAt;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public void setMerchantId(UUID merchantId) {
        this.merchantId = merchantId;
    }
}
