package com.leadflow.backend.entities.user;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "roles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_roles_name", columnNames = "name")
        }
)
public class Role implements Serializable {

    private static final long serialVersionUID = 1L;

    /* ======================================================
       ID
       ====================================================== */

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /* ======================================================
       FIELDS
       ====================================================== */

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /* ======================================================
       AUDIT
       ====================================================== */

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /* ======================================================
       CONSTRUCTORS
       ====================================================== */

    protected Role() {
        // Required by JPA
    }

    public Role(String name) {
        this.name = normalizeAndValidate(name);
    }

    /* ======================================================
       NORMALIZATION & VALIDATION
       ====================================================== */

    private String normalizeAndValidate(String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Role name cannot be null or blank");
        }

        String normalized = value.trim().toUpperCase();

        if (normalized.length() > 50) {
            throw new IllegalArgumentException("Role name exceeds 50 characters");
        }

        return normalized;
    }

    /* ======================================================
       LIFECYCLE
       ====================================================== */

    @PreUpdate
    @PrePersist
    private void normalize() {
        if (name != null) {
            name = normalizeAndValidate(name);
        }
    }

    /* ======================================================
       GETTERS
       ====================================================== */

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /* ======================================================
       EQUALS & HASHCODE (Hibernate-safe)
       ====================================================== */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role role)) return false;
        return id != null && id.equals(role.id);
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
        return "Role{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}