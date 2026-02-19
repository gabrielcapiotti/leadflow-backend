package com.leadflow.backend.entities.lead;

import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "lead_status_history",
    indexes = {
        @Index(name = "idx_lsh_lead_id", columnList = "lead_id")
    }
)
public class LeadStatusHistory {

    @Id
    @GeneratedValue
    private UUID id;

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

    /*
     * Histórico NÃO deve ser mutável.
     * Removido updated_at — não faz sentido versionar histórico.
     */

    /* ==========================
       CONSTRUTORES
       ========================== */

    protected LeadStatusHistory() {
        // JPA only
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
        this.changedBy = changedBy;
    }

    /* ==========================
       GETTERS
       ========================== */

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

    /**
     * Alias semântico para DTO
     */
    public User getUpdatedBy() {
        return changedBy;
    }

    /* ==========================
       IMUTABILIDADE
       ========================== */

    // Histórico não deve ser alterado após criação.
    // Nenhum setter público.

    protected void setLead(Lead lead) {
        this.lead = lead;
    }

    /* ==========================
       EQUALS & HASHCODE (JPA SAFE)
       ========================== */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LeadStatusHistory other)) return false;
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
        return "LeadStatusHistory{" +
               "id=" + id +
               ", status=" + status +
               ", changedAt=" + changedAt +
               '}';
    }
}
