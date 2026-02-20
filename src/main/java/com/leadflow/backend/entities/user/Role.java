package com.leadflow.backend.entities.user;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "roles",
    schema = "public",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_roles_name", columnNames = "name")
    }
)
public class Role {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 50, updatable = false)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /* ==========================
       CONSTRUTORES
       ========================== */

    protected Role() {
        // JPA only
    }

    public Role(String name) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Role name cannot be null or blank");
        }

        this.name = normalize(name);
    }

    /* ==========================
       NORMALIZAÇÃO
       ========================== */

    private String normalize(String value) {
        return value.trim().toUpperCase();
    }

    /* ==========================
       GETTERS (imutável)
       ========================== */

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

    /* ==========================
       EQUALS & HASHCODE (JPA SAFE)
       ========================== */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /* ==========================
       TO STRING
       ========================== */

    @Override
    public String toString() {
        return "Role{" +
               "id=" + id +
               ", name='" + name + '\'' +
               '}';
    }
}
