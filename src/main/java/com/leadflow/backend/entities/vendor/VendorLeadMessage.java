package com.leadflow.backend.entities.vendor;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "vendor_lead_messages",
        indexes = {
                @Index(name = "idx_vlm_vendor_lead_id", columnList = "vendor_lead_id"),
                @Index(name = "idx_vlm_vendor_lead_created_at", columnList = "vendor_lead_id,created_at")
        }
)
public class VendorLeadMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID vendorLeadId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversationRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public VendorLeadMessage() {
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getVendorLeadId() {
        return vendorLeadId;
    }

    public ConversationRole getRole() {
        return role;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setVendorLeadId(UUID vendorLeadId) {
        this.vendorLeadId = vendorLeadId;
    }

    public void setRole(ConversationRole role) {
        this.role = role;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}