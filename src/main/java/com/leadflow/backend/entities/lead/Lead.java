package com.leadflow.backend.entities.lead;

import com.leadflow.backend.entities.enums.LeadStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "leads",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_leads_email_user",
                        columnNames = {"email", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_leads_email", columnList = "email"),
                @Index(name = "idx_leads_user", columnList = "user_id"),
                @Index(name = "idx_leads_user_email", columnList = "user_id,email")
        }
)
public class Lead {

    /* ======================================================
       ID
       ====================================================== */

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /* ======================================================
       RELATIONSHIPS (SCHEMA-BASED MULTI-TENANT)
       ====================================================== */

    /**
     * Referência apenas por ID.
     * Não existe relacionamento JPA com User
     * para evitar dependência cross-schema.
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /* ======================================================
       FIELDS
       ====================================================== */

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "phone", length = 15)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private LeadStatus status = LeadStatus.NEW;

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

    protected Lead() {
        // JPA only
    }

    public Lead(UUID userId, String name, String email, String phone) {
        setUserId(userId);
        setName(name);
        setEmail(email);
        this.phone = phone;
        this.status = LeadStatus.NEW;
    }

    /* ======================================================
       GETTERS
       ====================================================== */

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public LeadStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }

    /* ======================================================
       CONTROLLED SETTERS
       ====================================================== */

    private void setUserId(UUID userId) {
        if (userId == null)
            throw new IllegalArgumentException("UserId cannot be null");
        this.userId = userId;
    }

    private void setName(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Name cannot be blank");
        this.name = name.trim();
    }

    private void setEmail(String email) {
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Email cannot be blank");

        this.email = email.trim().toLowerCase();
    }

    /* ======================================================
       BUSINESS METHODS
       ====================================================== */

    public void changeStatus(LeadStatus newStatus) {

        if (newStatus == null)
            throw new IllegalArgumentException("Status cannot be null");

        if (!this.status.canTransitionTo(newStatus))
            throw new IllegalArgumentException(
                    "Invalid status transition from " +
                            this.status + " to " + newStatus
            );

        this.status = newStatus;
    }

    public void softDelete() {
        if (deletedAt == null) {
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
       EQUALS & HASHCODE (JPA SAFE)
       ====================================================== */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Lead other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}