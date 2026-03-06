package com.leadflow.backend.entities.system;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "system_audit_logs")
public class SystemAuditLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String entity;

    private String entityId;

    @Column(length = 2000)
    private String details;

    private String tenant;

    private String performedBy;

    @Column(nullable = false)
    private Instant createdAt;

    public SystemAuditLog() {}

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getTenant() {
        return tenant;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    // getters e setters
}