package com.leadflow.backend.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "tenants",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_tenants_schema_name", columnNames = "schema_name")
    },
    indexes = {
        @Index(name = "idx_tenants_schema_name", columnList = "schema_name")
    }
)
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "schema_name", nullable = false, length = 100)
    private String schemaName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /* ==========================
       CONSTRUCTORS
       ========================== */

    protected Tenant() {
        // JPA only
    }

    public Tenant(String name, String schemaName) {
        validateName(name);
        validateSchema(schemaName);

        this.name = name.trim();
        this.schemaName = normalizeSchema(schemaName);
    }

    /* ==========================
       GETTERS
       ========================== */

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

    /* ==========================
       DOMAIN RULES
       ========================== */

    public void rename(String newName) {
        validateName(newName);
        this.name = newName.trim();
    }

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
            throw new IllegalArgumentException("Schema 'public' is reserved");
        }
    }

    private String normalizeSchema(String schema) {
        return schema.trim().toLowerCase();
    }

    /* ==========================
       EQUALS & HASHCODE (JPA SAFE)
       ========================== */

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

    /* ==========================
       TO STRING
       ========================== */

    @Override
    public String toString() {
        return "Tenant{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", schemaName='" + schemaName + '\'' +
                '}';
    }
}
