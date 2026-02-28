package com.leadflow.domain.auth;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import com.leadflow.backend.entities.user.User;

@Entity
@Table(
    name = "password_reset_token",
    indexes = {
        @Index(name = "idx_password_reset_token_hash", columnList = "token_hash"),
        @Index(name = "idx_password_reset_token_user", columnList = "user_id"),
        @Index(name = "idx_password_reset_token_expires", columnList = "expires_at")
    }
)
public class PasswordResetToken {

    /* ======================================================
       ID
       ====================================================== */

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    /* ======================================================
       TOKEN (HASHED)
       ====================================================== */

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    /* ======================================================
       RELATIONSHIP
       ====================================================== */

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /* ======================================================
       EXPIRATION
       ====================================================== */

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    /* ======================================================
       AUDIT
       ====================================================== */

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /* ======================================================
       CONSTRUCTORS
       ====================================================== */

    protected PasswordResetToken() {
        // Required by JPA
    }

    public PasswordResetToken(
            String tokenHash,
            User user,
            LocalDateTime expiresAt
    ) {
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException("Token hash cannot be blank");
        }

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Expiration must be in the future");
        }

        this.tokenHash = tokenHash;
        this.user = user;
        this.expiresAt = expiresAt;
        this.used = false;
        this.createdAt = LocalDateTime.now();
    }

    /* ======================================================
       BUSINESS METHODS
       ====================================================== */

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public void markAsUsed() {
        if (this.used) {
            throw new IllegalStateException("Token already used");
        }
        this.used = true;
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

    public User getUser() {
        return user;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /* ======================================================
       EQUALS & HASHCODE (Hibernate-safe)
       ====================================================== */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PasswordResetToken other)) return false;
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
        return "PasswordResetToken{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", expiresAt=" + expiresAt +
                ", used=" + used +
                '}';
    }
}