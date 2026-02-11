package com.leadflow.backend.entities.lead;

import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
    name = "lead_status_history",
    indexes = {
        @Index(
            name = "idx_lsh_lead_id",
            columnList = "lead_id"
        )
    }
)
public class LeadStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ==========================
       RELACIONAMENTOS
       ========================== */

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "lead_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_lsh_lead")
    )
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "changed_by",
        foreignKey = @ForeignKey(name = "fk_lsh_user")
    )
    private User changedBy;

    /* ==========================
       CAMPOS
       ========================== */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadStatus status;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /* ==========================
       CONSTRUTORES
       ========================== */

    protected LeadStatusHistory() {
        // JPA only
    }

    public LeadStatusHistory(Lead lead, LeadStatus status, User changedBy) {
        this.lead = lead;
        this.status = status;
        this.changedBy = changedBy;
    }

    /* ==========================
       GETTERS
       ========================== */

    public Long getId() {
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

    /**
     * Alias semântico para controllers/DTOs
     */
    public User getUpdatedBy() {
        return changedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /* ==========================
       EQUALS & HASHCODE
       ========================== */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LeadStatusHistory)) return false;
        LeadStatusHistory that = (LeadStatusHistory) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /* ==========================
       TO STRING
       ========================== */

    @Override
    public String toString() {
        return "LeadStatusHistory{" +
               "id=" + id +
               ", status=" + status +
               ", changedAt=" + changedAt +
               '}';
    }

    public void setLead(Lead created) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setLead'");
    }
}
