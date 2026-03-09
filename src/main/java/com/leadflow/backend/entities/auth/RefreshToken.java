package com.leadflow.backend.entities.auth;

import com.leadflow.backend.entities.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_refresh_tokens_user", columnList = "user_id"),
                @Index(name = "idx_refresh_tokens_hash", columnList = "token_hash"),
                @Index(name = "idx_refresh_tokens_fingerprint", columnList = "device_fingerprint"),
                @Index(name = "idx_refresh_tokens_expires", columnList = "expires_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_refresh_tokens_hash", columnNames = "token_hash")
        }
)
public class RefreshToken {

    /* ======================================================
       ID
       ====================================================== */

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /* ======================================================
       CORE FIELDS
       ====================================================== */

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "device_fingerprint", nullable = false, length = 255)
    private String deviceFingerprint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_refresh_tokens_user")
    )
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    /* ======================================================
       AUDIT
       ====================================================== */

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /* ======================================================
       CONSTRUCTORS
       ====================================================== */

    protected RefreshToken() {
        // JPA
    }

    public RefreshToken(
            String tokenHash,
            String deviceFingerprint,
            User user,
            LocalDateTime expiresAt
    ) {

        if (tokenHash == null || tokenHash.isBlank())
            throw new IllegalArgumentException("Token hash cannot be blank");

        if (deviceFingerprint == null || deviceFingerprint.isBlank())
            throw new IllegalArgumentException("Device fingerprint cannot be blank");

        if (user == null)
            throw new IllegalArgumentException("User cannot be null");

        if (expiresAt == null || expiresAt.isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Expiration must be in the future");

        this.tokenHash = tokenHash;
        this.deviceFingerprint = deviceFingerprint;
        this.user = user;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    /* ======================================================
       BUSINESS RULES
       ====================================================== */

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }

    public void revoke() {
        this.revoked = true;
    }

    /* ======================================================
       GETTERS
       ====================================================== */

    public UUID getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public User getUser() {
        return user;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /* ======================================================
       SETTERS (controlled)
       ====================================================== */

    public void setDeviceFingerprint(String deviceFingerprint) {
        if (deviceFingerprint == null || deviceFingerprint.isBlank())
            throw new IllegalArgumentException("Device fingerprint cannot be blank");

        this.deviceFingerprint = deviceFingerprint;
    }

    /* ======================================================
       EQUALS & HASHCODE (Hibernate-safe)
       ====================================================== */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RefreshToken)) return false;
        RefreshToken that = (RefreshToken) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /* ======================================================
       TO STRING (SAFE)
       ====================================================== */

    @Override
    public String toString() {
        return "RefreshToken{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", revoked=" + revoked +
                '}';
    }
}