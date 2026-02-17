package com.leadflow.backend.entities.user;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100, updatable = false)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "role_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_users_role")
    )
    private Role role;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /* ======================================================
       CONSTRUTORES
       ====================================================== */

    protected User() {
        // JPA only
    }

    public User(String name, String email, String encryptedPassword, Role role) {

        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Name cannot be null or blank");

        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Email cannot be null or blank");

        if (encryptedPassword == null || encryptedPassword.isBlank())
            throw new IllegalArgumentException("Password cannot be null or blank");

        if (role == null)
            throw new IllegalArgumentException("Role cannot be null");

        this.name = name.trim();
        this.email = normalizeEmail(email);
        this.password = encryptedPassword;
        this.role = role;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    /* ======================================================
       GETTERS
       ====================================================== */

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
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

    /* ======================================================
       MULTITENANCY
       ====================================================== */

    public String getTenantId() {
        // Caso ainda não use multitenancy real no User,
        // pode retornar tenant fixo ou null.
        return "public";
    }

    /* ======================================================
       REGRAS DE DOMÍNIO
       ====================================================== */

    public void changeName(String newName) {
        if (newName == null || newName.isBlank())
            throw new IllegalArgumentException("Name cannot be blank");

        this.name = newName.trim();
    }

    public void changePassword(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isBlank())
            throw new IllegalArgumentException("Password cannot be blank");

        this.password = encryptedPassword;
    }

    public void changeRole(Role newRole) {
        if (newRole == null)
            throw new IllegalArgumentException("Role cannot be null");

        this.role = newRole;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deletedAt = null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /* ======================================================
       SETTERS CONTROLADOS (COMPATIBILIDADE)
       ====================================================== */

    public void setName(String name) {
        changeName(name);
    }

    public void setRole(Role role) {
        changeRole(role);
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    /**
     * ⚠️ Email não deveria ser alterado.
     * Mantido apenas para compatibilidade.
     */
    public void setEmail(String email) {
        this.email = normalizeEmail(email);
    }

    /* ======================================================
       EQUALS & HASHCODE
       ====================================================== */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /* ======================================================
       TO STRING (SEM SENHA)
       ====================================================== */

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role=" + (role != null ? role.getName() : null) +
                '}';
    }
}
