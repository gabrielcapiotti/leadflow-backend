package com.leadflow.backend.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Entity
@Table(
        name = "tenants",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_tenants_schema_name",
                        columnNames = "schema_name"
                )
        },
        indexes = {
                @Index(
                        name = "idx_tenants_schema_name",
                        columnList = "schema_name"
                )
        }
)
public class Tenant {

    private static final Pattern VALID_SCHEMA =
            Pattern.compile("^[a-z][a-z0-9_]{2,49}$");

    /* ======================================================
       ID
       ====================================================== */

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    /* ======================================================
       FIELDS
       ====================================================== */

    @Column(nullable = false, length = 100)
    private String name;

    @Column(
            name = "schema_name",
            nullable = false,
            length = 50,
            updatable = false
    )
    private String schemaName;

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

    protected Tenant() {
    }

    public Tenant(String name, String schemaName) {
        setName(name);
        setSchemaName(schemaName);
    }

    /* ======================================================
       GETTERS
       ====================================================== */

    public UUID getId() { return id; }

    public String getName() { return name; }

    public String getSchemaName() { return schemaName; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }

    /* ======================================================
       DOMAIN BEHAVIOR
       ====================================================== */

    public void rename(String newName) {
        setName(newName);
    }

    public void softDelete() {
        if (this.deletedAt == null) {
            this.deletedAt = LocalDateTime.now();
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /* ======================================================
       VALIDATION
       ====================================================== */

    private void setName(String name) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tenant name cannot be blank");
        }

        if (name.length() > 100) {
            throw new IllegalArgumentException("Tenant name too long");
        }

        this.name = name.trim();
    }

    private void setSchemaName(String schema) {

        if (schema == null || schema.isBlank()) {
            throw new IllegalArgumentException("Schema name cannot be blank");
        }

        String normalized = schema.trim().toLowerCase();

        if (!VALID_SCHEMA.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Invalid schema format"
            );
        }

        if ("public".equals(normalized)) {
            throw new IllegalArgumentException(
                    "Schema 'public' is reserved"
            );
        }

        this.schemaName = normalized;
    }

    /* ======================================================
       EQUALS & HASHCODE
       ====================================================== */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tenant other)) return false;
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
        return "Tenant{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", schemaName='" + schemaName + '\'' +
                ", deletedAt=" + deletedAt +
                '}';
    }
}