package com.leadflow.backend.entities.auth;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "user_sessions",
    indexes = {
        @Index(name = "idx_session_token_tenant", columnList = "tokenId, tenantId"),
        @Index(name = "idx_session_user_tenant", columnList = "userId, tenantId"),
        @Index(name = "idx_session_active", columnList = "active"),
        @Index(name = "idx_session_last_access", columnList = "lastAccessAt")
    }
)
public class UserSession {

    /* ======================================================
       IDENTITY
       ====================================================== */

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String tokenId;

    /* ======================================================
       DEVICE INFORMATION
       ====================================================== */

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 512)
    private String userAgent;

    @Column(length = 45, updatable = false)
    private String initialIpAddress;

    @Column(length = 512, updatable = false)
    private String initialUserAgent;

    /* ======================================================
       SESSION STATE
       ====================================================== */

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean suspicious = false;

    /* ======================================================
       TIMESTAMPS
       ====================================================== */

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant lastAccessAt;

    private Instant revokedAt;

    /* ======================================================
       LIFECYCLE HOOK
       ====================================================== */

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (lastAccessAt == null) {
            lastAccessAt = createdAt;
        }
    }

    /* ======================================================
       GETTERS
       ====================================================== */

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getTokenId() {
        return tokenId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getInitialIpAddress() {
        return initialIpAddress;
    }

    public String getInitialUserAgent() {
        return initialUserAgent;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isSuspicious() {
        return suspicious;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastAccessAt() {
        return lastAccessAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    /* ======================================================
       MUTATORS CONTROLADOS
       ====================================================== */

    public void activate() {
        this.active = true;
    }

    public void revoke(Instant timestamp) {
        this.active = false;
        this.revokedAt = timestamp;
    }

    public void markSuspicious() {
        this.suspicious = true;
    }

    public void updateActivity(Instant timestamp) {
        this.lastAccessAt = timestamp;
    }

    public void updateDeviceInfo(String ip, String agent) {
        this.ipAddress = ip;
        this.userAgent = agent;
    }

    public void setInitialDeviceInfo(String ip, String agent) {
        this.initialIpAddress = ip;
        this.initialUserAgent = agent;
    }

    public UserSession(UUID userId,
                   UUID tenantId,
                   String tokenId,
                   String ipAddress,
                   String userAgent,
                   Instant now) {

    this.userId = userId;
    this.tenantId = tenantId;
    this.tokenId = tokenId;

    this.initialIpAddress = ipAddress;
    this.initialUserAgent = userAgent;

    this.ipAddress = ipAddress;
    this.userAgent = userAgent;

    this.active = true;
    this.suspicious = false;

    this.createdAt = now;
    this.lastAccessAt = now;
}

protected UserSession() {}
}