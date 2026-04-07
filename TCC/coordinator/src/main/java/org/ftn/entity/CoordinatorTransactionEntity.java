package org.ftn.entity;

import jakarta.persistence.*;
import org.ftn.constant.CoordinatorTransactionState;
import org.ftn.constant.Decision;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coordinator_transaction_entity")
@Access(AccessType.PROPERTY)
public class CoordinatorTransactionEntity {
    private UUID id;
    private CoordinatorTransactionState state;
    private Decision decision;
    private Instant createdAt;
    private ParticipantDataEntity participantData;
    private String abortReason;
    private boolean completed;

    public CoordinatorTransactionEntity() {

    }

    public CoordinatorTransactionEntity(UUID id, CoordinatorTransactionState state, Decision decision, Instant createdAt, boolean completed) {
        this.id = id;
        this.state = state;
        this.decision = decision;
        this.createdAt = createdAt;
        this.completed = completed;
        this.participantData = new ParticipantDataEntity();
    }

    @Id
    @GeneratedValue
    public UUID getId() {
        return id;
    }

    @Enumerated(EnumType.STRING)
    public CoordinatorTransactionState getState() {
        return state;
    }

    @Enumerated(EnumType.STRING)
    public Decision getDecision() {
        return decision;
    }

    @Column(name = "created_at")
    public Instant getCreatedAt() {
        return createdAt;
    }

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "participant_data_id")
    public ParticipantDataEntity getParticipantData() {
        return participantData;
    }

    @Column(name = "abort_reason")
    public String getAbortReason() {
        return abortReason;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setState(CoordinatorTransactionState state) {
        this.state = state;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setParticipantData(ParticipantDataEntity participantData) {
        this.participantData = participantData;
    }

    public void setAbortReason(String abortReason) {
        this.abortReason = abortReason;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
