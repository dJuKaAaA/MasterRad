package org.ftn.entity;

import jakarta.persistence.*;
import org.ftn.constant.SagaState;
import org.ftn.constant.TransactionState;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saga_entity")
@Access(AccessType.PROPERTY)
public class SagaEntity {
    private UUID id;
    private SagaState state;
    private Instant createdAt;
    private Instant lastUpdated;
    private String failureReason;
    private TransactionState transactionState;
    private RecoveryDataEntity recoveryData;
    private UUID idempotencyKey;

    public SagaEntity() {

    }

    public SagaEntity(UUID id, SagaState state, Instant createdAt, Instant lastUpdated, String failureReason, TransactionState transactionState, RecoveryDataEntity recoveryData, UUID idempotencyKey) {
        this.id = id;
        this.state = state;
        this.createdAt = createdAt;
        this.lastUpdated = lastUpdated;
        this.failureReason = failureReason;
        this.transactionState = transactionState;
        this.recoveryData = recoveryData;
        this.idempotencyKey = idempotencyKey;
    }

    @Id
    @GeneratedValue
    public UUID getId() {
        return id;
    }

    @Enumerated(value = EnumType.STRING)
    public SagaState getState() {
        return state;
    }

    @Column(name = "created_at")
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Column(name = "last_updated")
    public Instant getLastUpdated() {
        return lastUpdated;
    }

    @Column(name = "failure_reason")
    public String getFailureReason() {
        return failureReason;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_state")
    public TransactionState getTransactionState() {
        return transactionState;
    }

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "recovery_data_id")
    public RecoveryDataEntity getRecoveryData() {
        return recoveryData;
    }

    @Column(name = "idempotency_key", unique = true)
    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setState(SagaState state) {
        this.state = state;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public void setTransactionState(TransactionState transactionState) {
        this.transactionState = transactionState;
    }

    public void setRecoveryData(RecoveryDataEntity recoveryData) {
        this.recoveryData = recoveryData;
    }

    public void setIdempotencyKey(UUID idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
