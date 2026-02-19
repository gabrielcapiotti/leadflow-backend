package com.leadflow.backend.entities.lead;

import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.user.User;
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
            name = "uk_leads_email_user_tenant",
            columnNames = {"email", "user_id", "tenant_id"}
        )
    },
    indexes = {
        @Index(name = "idx_leads_email", columnList = "email"),
        @Index(name = "idx_leads_user", columnList = "user_id"),
        @Index(name = "idx_leads_user_email", columnList = "user_id,email"),
        @Index(name = "idx_leads_tenant", columnList = "tenant_id")
    }
)
public class Lead {

    /* ======================================================
       ID
       ====================================================== */

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    /* ======================================================
       RELATIONSHIPS
       ====================================================== */

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_leads_user")
    )
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
        name = "tenant_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_leads_tenant")
    )
    private Tenant tenant;

    /* ======================================================
       FIELDS
       ====================================================== */

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(length = 15)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
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

    protected Lead() {}

    public Lead(String name, String email, String phone) {

        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Name cannot be blank");

        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Email cannot be blank");

        this.name = name.trim();
        this.email = email.trim().toLowerCase();
        this.phone = phone;
        this.status = LeadStatus.NEW;
    }

    /* ======================================================
       GETTERS
       ====================================================== */

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public Tenant getTenant() { return tenant; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public LeadStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }

    /* ======================================================
       MULTI-TENANT RULE
       ====================================================== */

    public void setUser(User user) {
        if (user == null)
            throw new IllegalArgumentException("User cannot be null");

        if (user.getTenant() == null)
            throw new IllegalStateException("User must belong to a tenant");

        this.user = user;
        this.tenant = user.getTenant(); // sincroniza automaticamente
    }

    public void setTenant(Tenant tenant) {
        if (tenant == null)
            throw new IllegalArgumentException("Tenant cannot be null");

        this.tenant = tenant;
    }

    /* ======================================================
       BUSINESS METHODS
       ====================================================== */

    public void updateContact(String name, String email, String phone) {

        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Name cannot be blank");

        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Email cannot be blank");

        this.name = name.trim();
        this.email = email.trim().toLowerCase();
        this.phone = phone;
    }

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
        if (deletedAt == null)
            this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deletedAt = null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /* ======================================================
       JPA SAFE EQUALS / HASHCODE
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

    @Override
    public String toString() {
        return "Lead{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", email='" + email + '\'' +
               ", status=" + status +
               '}';
    }
}
