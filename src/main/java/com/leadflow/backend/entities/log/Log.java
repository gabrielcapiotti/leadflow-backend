package com.leadflow.backend.entities.log;

import com.leadflow.backend.entities.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "logs",
    indexes = {
        @Index(name = "idx_logs_user_id", columnList = "user_id"),
        @Index(name = "idx_logs_created_at", columnList = "created_at")
    }
)
public class Log {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Usuário que realizou a ação (pode ser null – ex: sistema)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "user_id",
        foreignKey = @ForeignKey(name = "fk_logs_user")
    )
    private User user;

    /*
     * Descrição da ação executada
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String action;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /* ==========================
       CONSTRUTORES
       ========================== */

    protected Log() {
        // JPA only
    }

    public Log(User user, String action) {

        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Action cannot be null or blank");
        }

        this.user = user;
        this.action = action.trim();
    }

    /* ==========================
       GETTERS (imutável)
       ========================== */

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getAction() {
        return action;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /* ==========================
       EQUALS & HASHCODE (JPA SAFE)
       ========================== */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Log other)) return false;
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
        return "Log{" +
               "id=" + id +
               ", action='" + action + '\'' +
               ", createdAt=" + createdAt +
               '}';
    }
}
