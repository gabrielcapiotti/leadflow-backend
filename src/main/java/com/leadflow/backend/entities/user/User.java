package com.leadflow.backend.entities.user;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_users_email", columnNames = "email")
        }
)
public class User {

    /* ======================================================
       CONFIGURAÇÕES DE SEGURANÇA
       ====================================================== */

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    /* ======================================================
       ID
       ====================================================== */

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    /* ======================================================
       FIELDS
       ====================================================== */

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    /* ======================================================
       BLOQUEIO DE LOGIN
       ====================================================== */

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "lock_until")
    private LocalDateTime lockUntil;

    /* ======================================================
       RELATIONSHIPS
       ====================================================== */

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "role_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_users_role")
    )
    private Role role;

    /* ======================================================
       AUDIT
       ====================================================== */

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /* ======================================================
       CONSTRUCTORS
       ====================================================== */

    protected User() {
        // Required by JPA
    }

    public User(String name,
                String email,
                String encryptedPassword,
                Role role) {

        validate(name, email, encryptedPassword, role);

        this.name = name.trim();
        this.email = normalizeEmail(email);
        this.password = encryptedPassword;
        this.role = role;
    }

    /* ======================================================
       VALIDATION
       ====================================================== */

    private void validate(String name,
                          String email,
                          String password,
                          Role role) {

        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Name cannot be blank");

        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Email cannot be blank");

        if (password == null || password.isBlank())
            throw new IllegalArgumentException("Password cannot be blank");

        if (role == null)
            throw new IllegalArgumentException("Role cannot be null");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    /* ======================================================
       GETTERS
       ====================================================== */

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }

    public int getFailedAttempts() { return failedAttempts; }
    public LocalDateTime getLockUntil() { return lockUntil; }

    /* ======================================================
       DOMAIN METHODS
       ====================================================== */

    public void changeName(String newName) {
        if (newName == null || newName.isBlank())
            throw new IllegalArgumentException("Name cannot be blank");

        this.name = newName.trim();
    }

    public void changeEmail(String newEmail) {
        if (newEmail == null || newEmail.isBlank())
            throw new IllegalArgumentException("Email cannot be blank");

        this.email = normalizeEmail(newEmail);
    }

    public void changeRole(Role newRole) {
        if (newRole == null)
            throw new IllegalArgumentException("Role cannot be null");

        this.role = newRole;
    }

    public void changePassword(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be blank");
        }
        this.password = encodedPassword;
    }

    /* ======================================================
       LOGIN SECURITY DOMAIN LOGIC
       ====================================================== */

    public void registerFailedLogin() {

        if (isAccountLocked()) {
            return;
        }

        this.failedAttempts++;

        if (this.failedAttempts >= MAX_FAILED_ATTEMPTS) {
            this.lockUntil = LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES);
        }
    }

    public void resetLoginAttempts() {
        this.failedAttempts = 0;
        this.lockUntil = null;
    }

    public boolean isAccountLocked() {
        return lockUntil != null && lockUntil.isAfter(LocalDateTime.now());
    }

    /* ======================================================
       SOFT DELETE
       ====================================================== */

    public void softDelete() {
        if (this.deletedAt == null) {
            this.deletedAt = LocalDateTime.now();
        }
    }

    public void restore() {
        this.deletedAt = null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /* ======================================================
       EQUALS & HASHCODE (Hibernate-safe)
       ====================================================== */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /* ======================================================
       TO STRING
       ====================================================== */

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                '}';
    }
}