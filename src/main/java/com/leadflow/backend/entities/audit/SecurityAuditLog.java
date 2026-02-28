package com.leadflow.backend.entities.audit;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "security_audit_logs",
        indexes = {
                @Index(name = "idx_audit_email", columnList = "email"),
                @Index(name = "idx_audit_tenant", columnList = "tenant"),
                @Index(name = "idx_audit_created_at", columnList = "created_at")
        }
)
public class SecurityAuditLog {

    /* ======================================================
       ID
       ====================================================== */

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /* ======================================================
       ACTION
       ====================================================== */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SecurityAction action;

    /* ======================================================
       CONTEXT
       ====================================================== */

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 100)
    private String tenant;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    /* ======================================================
       AUDIT
       ====================================================== */

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /* ======================================================
       CONSTRUCTORS
       ====================================================== */

    protected SecurityAuditLog() {
        // Required by JPA
    }

    public SecurityAuditLog(
            SecurityAction action,
            String email,
            String tenant,
            boolean success,
            String ipAddress,
            String userAgent,
            String correlationId
    ) {
        this.action = Objects.requireNonNull(action);
        this.email = normalize(email);
        this.tenant = normalize(tenant);
        this.success = success;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.correlationId = correlationId;
    }

    /* ======================================================
       NORMALIZATION
       ====================================================== */

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    /* ======================================================
       GETTERS
       ====================================================== */

    public UUID getId() { return id; }
    public SecurityAction getAction() { return action; }
    public String getEmail() { return email; }
    public String getTenant() { return tenant; }
    public boolean isSuccess() { return success; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public String getCorrelationId() { return correlationId; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    /* ======================================================
       EQUALS & HASHCODE
       ====================================================== */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SecurityAuditLog other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /* ======================================================
       TO STRING (Safe)
       ====================================================== */

    @Override
    public String toString() {
        return "SecurityAuditLog{" +
                "id=" + id +
                ", action=" + action +
                ", email='" + email + '\'' +
                ", tenant='" + tenant + '\'' +
                ", success=" + success +
                ", createdAt=" + createdAt +
                '}';
    }
}