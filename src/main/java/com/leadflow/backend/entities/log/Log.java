package com.leadflow.backend.entities.log;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.leadflow.backend.entities.user.User;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "logs")
public class Log {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuário que realizou a ação (pode ser null)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "user_id",
        foreignKey = @ForeignKey(name = "fk_logs_user")
    )
    private User user;

    /**
     * Descrição da ação executada
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String action;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /* ==========================
       CONSTRUTORES
       ========================== */

    protected Log() {
        // JPA only
    }

    public Log(User user, String action) {
        this.user = user;
        this.action = action;
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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
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
        if (!(o instanceof Log)) return false;
        Log log = (Log) o;
        return Objects.equals(id, log.id);
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
        return "Log{" +
               "id=" + id +
               ", action='" + action + '\'' +
               ", createdAt=" + createdAt +
               '}';
    }
}
