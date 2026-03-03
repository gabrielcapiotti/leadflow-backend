package com.leadflow.backend.entities.vendor;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vendor_lead_stage_history")
public class VendorLeadStageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID vendorLeadId;

    @Column(nullable = false)
    private String previousStage;

    @Column(nullable = false)
    private String newStage;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    public VendorLeadStageHistory() {}

    @PrePersist
    public void onCreate() {
        this.changedAt = Instant.now();
    }

    // GETTERS

    public UUID getId() { return id; }
    public UUID getVendorLeadId() { return vendorLeadId; }
    public String getPreviousStage() { return previousStage; }
    public String getNewStage() { return newStage; }
    public Instant getChangedAt() { return changedAt; }

    // SETTERS

    public void setVendorLeadId(UUID vendorLeadId) {
        this.vendorLeadId = vendorLeadId;
    }

    public void setPreviousStage(String previousStage) {
        this.previousStage = previousStage;
    }

    public void setNewStage(String newStage) {
        this.newStage = newStage;
    }
}