package com.leadflow.backend.entities.lead;

import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "lead_status_history",
        indexes = {
                @Index(name = "idx_lsh_lead_id", columnList = "lead_id"),
                @Index(name = "idx_lsh_lead_changed_at", columnList = "lead_id,changed_at")
        }
)
public class LeadStatusHistory {

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "lead_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_lsh_lead")
    )
    private Lead lead;

    /**
     * Usuário que alterou o status.
     * Pode ser null quando alteração é SYSTEM.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
            name = "changed_by",
            foreignKey = @ForeignKey(name = "fk_lsh_user")
    )
    private User changedBy;

    /* ======================================================
       FIELDS
       ====================================================== */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LeadStatus status;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    /* ======================================================
       CONSTRUCTORS
       ====================================================== */

    protected LeadStatusHistory() {
        // Required by JPA
    }

    public LeadStatusHistory(Lead lead, LeadStatus status, User changedBy) {

        if (lead == null) {
            throw new IllegalArgumentException("Lead cannot be null");
        }

        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        this.lead = lead;
        this.status = status;
        this.changedBy = changedBy; // Pode ser null (SYSTEM)
    }

    /* ======================================================
       GETTERS
       ====================================================== */

    public UUID getId() {
        return id;
    }

    public Lead getLead() {
        return lead;
    }

    public LeadStatus getStatus() {
        return status;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public User getChangedBy() {
        return changedBy;
    }

    /* ======================================================
       EQUALS & HASHCODE (Hibernate-safe)
       ====================================================== */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LeadStatusHistory other)) return false;
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
        return "LeadStatusHistory{" +
                "id=" + id +
                ", status=" + status +
                ", changedAt=" + changedAt +
                '}';
    }
}