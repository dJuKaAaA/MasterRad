package org.ftn.payment.entity;

import io.quarkus.agroal.DataSource;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "wallet_entity")
@Access(AccessType.PROPERTY)
@DataSource("payment")
public class WalletEntity {
    private UUID id;
    private UUID userId;
    private BigDecimal balance;

    @Id
    @GeneratedValue
    public UUID getId() {
        return id;
    }

    @Column(name = "user_id", unique = true)
    public UUID getUserId() {
        return userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void pay(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }

    public void refund(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }
}
