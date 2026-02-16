package com.leadflow.backend.entities.lead;

import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.entities.enums.LeadStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
    name = "leads",
    indexes = {
        @Index(name = "idx_leads_email", columnList = "email"),
        @Index(name = "idx_leads_user", columnList = "user_id")
    }
)
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ==========================
       RELACIONAMENTO
       ========================== */

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /* ==========================
       CAMPOS
       ========================== */

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(nullable = false, length = 15)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadStatus status = LeadStatus.NEW;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /* ==========================
       CONSTRUTORES
       ========================== */

    public Lead() {
        // JPA only
    }

    public Lead(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.status = LeadStatus.NEW;
    }

    /* ==========================
       GETTERS & SETTERS
       ========================== */

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LeadStatus getStatus() {
        return status;
    }

    public void setStatus(LeadStatus status) {
        this.status = status;
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

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    /* ==========================
       DOMÍNIO
       ========================== */

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void changeStatus(LeadStatus newStatus) {

        if (newStatus == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        if (this.status == newStatus) {
            return; // mesmo status permitido, sem histórico
        }

        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalArgumentException(
                    "Invalid status transition from " +
                    this.status + " to " + newStatus
            );
        }

        this.status = newStatus;
    }

    /* ==========================
       EQUALS & HASHCODE
       ========================== */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Lead)) return false;
        Lead lead = (Lead) o;
        return Objects.equals(id, lead.id);
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
        return "Lead{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", email='" + email + '\'' +
               ", status=" + status +
               '}';
    }

    public void setId(long l) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setId'");
    }
}
