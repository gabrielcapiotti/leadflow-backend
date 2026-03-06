package com.leadflow.backend.entities.vendor;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vendor_lead_conversations")
public class VendorLeadConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID vendorLeadId;

    @Column(nullable = false)
    private UUID leadId;

    @Column(length = 5000, nullable = false)
    private String content;

    @Column(length = 20)
    private String role;

    @Column(length = 50)
    private String sender;

    @Column(length = 100)
    private String tenant;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /* ======================================================
       LIFECYCLE
       ====================================================== */

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /* ======================================================
       GETTERS
       ====================================================== */

    public UUID getId() {
        return id;
    }

    public UUID getVendorLeadId() {
        return vendorLeadId;
    }

    public UUID getLeadId() {
        return leadId;
    }

    public String getContent() {
        return content;
    }

    public String getRole() {
        return role;
    }

    public String getSender() {
        return sender;
    }

    public String getTenant() {
        return tenant;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /* ======================================================
       SETTERS
       ====================================================== */

    public void setVendorLeadId(UUID vendorLeadId) {
        this.vendorLeadId = vendorLeadId;
    }

    public void setLeadId(UUID leadId) {
        this.leadId = leadId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}