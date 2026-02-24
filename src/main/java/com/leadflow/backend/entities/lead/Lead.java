package com.leadflow.backend.entities.lead;

import com.leadflow.backend.entities.enums.LeadStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;
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
// Opcional para produção:
// @Where(clause = "deleted_at IS NULL")
public class Lead {

    /* ======================================================
       ID
       ====================================================== */

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    /* ======================================================
       RELATIONSHIPS (SCHEMA MULTI-TENANT SAFE)
       ====================================================== */

    /**
     * Apenas referência por ID.
     * Não existe relacionamento JPA com User.
     * Isolamento ocorre via schema + userId.
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /* ======================================================
       FIELDS
       ====================================================== */

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
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
        // Required by JPA
    }

    public Lead(UUID userId, String name, String email, String phone) {
        validateUserId(userId);
        this.userId = userId;
        setName(name);
        setEmail(email);
        this.phone = normalizePhone(phone);
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
       VALIDATION
       ====================================================== */

    private void validateUserId(UUID userId) {
        if (userId == null)
            throw new IllegalArgumentException("UserId cannot be null");
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

    private String normalizePhone(String phone) {
        return phone != null ? phone.trim() : null;
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
       EQUALS & HASHCODE (Hibernate-safe)
       ====================================================== */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Lead other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}