package com.leadflow.backend.entities.user;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "roles", // Nome da tabela
        schema = "public", // Define explicitamente o schema
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_roles_name", columnNames = "name")
        }
)
public class Role {

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

    @Column(nullable = false, length = 50, updatable = false)
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

    // Construtor padrão, necessário para o JPA
    protected Role() {
        // Required by JPA
    }

    // Construtor para inicializar a role com validação
    public Role(String name) {
        this.name = normalizeAndValidate(name);
    }

    /* ======================================================
       NORMALIZATION & VALIDATION
       ====================================================== */

    /**
     * Normaliza e valida o nome da role.
     * @param value Nome da role.
     * @return Nome validado e normalizado.
     * @throws IllegalArgumentException se o nome for inválido.
     */
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
        if (!(o instanceof Role other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id); // Melhor prática para gerar hashCode.
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