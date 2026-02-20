package com.leadflow.backend.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

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

    /* ======================================================
       ID
       ====================================================== */

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /* ======================================================
       FIELDS
       ====================================================== */

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "schema_name", nullable = false, length = 100)
    private String schemaName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /* ======================================================
       CONSTRUCTORS
       ====================================================== */

    protected Tenant() {
        // Required by JPA
    }

    public Tenant(String name, String schemaName) {
        setName(name);
        setSchemaName(schemaName);
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

    public String getSchemaName() {
        return schemaName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    /* ======================================================
       DOMAIN BEHAVIOR
       ====================================================== */

    public void rename(String newName) {
        setName(newName);
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /* ======================================================
       VALIDATION
       ====================================================== */

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tenant name cannot be blank");
        }
    }

    private void validateSchema(String schema) {

        if (schema == null || schema.isBlank()) {
            throw new IllegalArgumentException("Schema name cannot be blank");
        }

        if (!schema.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException(
                    "Schema name must contain only letters, numbers and underscore"
            );
        }

        if (schema.equalsIgnoreCase("public")) {
            throw new IllegalArgumentException(
                    "Schema 'public' is reserved"
            );
        }
    }

    private String normalizeSchema(String schema) {
        return schema.trim().toLowerCase();
    }

    /* ======================================================
       CONTROLLED SETTERS
       ====================================================== */

    private void setName(String name) {
        validateName(name);
        this.name = name.trim();
    }

    private void setSchemaName(String schema) {
        validateSchema(schema);
        this.schemaName = normalizeSchema(schema);
    }

    /* ======================================================
       EQUALS & HASHCODE (JPA SAFE)
       ====================================================== */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tenant other)) return false;
        return id != null && id.equals(other.id);
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