package com.leadflow.backend.entities.user;

import com.leadflow.backend.entities.Tenant;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_tenant", columnList = "tenant_id"),
                @Index(name = "idx_users_email_tenant", columnList = "email, tenant_id")
        }
)
public class User {

    /* ======================================================
       ID
       ====================================================== */

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    /* ======================================================
       FIELDS
       ====================================================== */

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    /* ======================================================
       RELATIONSHIPS
       ====================================================== */

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "role_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_users_role")
    )
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "tenant_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_users_tenant")
    )
    private Tenant tenant;

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

    protected User() {
        // JPA only
    }

    public User(String name,
                String email,
                String encryptedPassword,
                Role role,
                Tenant tenant) {

        validate(name, email, encryptedPassword, role, tenant);

        this.name = name.trim();
        this.email = normalizeEmail(email);
        this.password = encryptedPassword;
        this.role = role;
        this.tenant = tenant;
    }

    /* ======================================================
       VALIDATION
       ====================================================== */

    private void validate(String name,
                          String email,
                          String password,
                          Role role,
                          Tenant tenant) {

        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Name cannot be blank");

        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Email cannot be blank");

        if (password == null || password.isBlank())
            throw new IllegalArgumentException("Password cannot be blank");

        if (role == null)
            throw new IllegalArgumentException("Role cannot be null");

        if (tenant == null)
            throw new IllegalArgumentException("Tenant cannot be null");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    /* ======================================================
       GETTERS
       ====================================================== */

    public UUID getId() { return id; }

    public String getName() { return name; }

    public String getEmail() { return email; }

    public String getPassword() { return password; }

    public Role getRole() { return role; }

    public Tenant getTenant() { return tenant; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }

    /* ======================================================
       DOMAIN METHODS
       ====================================================== */

    public void changeName(String newName) {
        if (newName == null || newName.isBlank())
            throw new IllegalArgumentException("Name cannot be blank");

        this.name = newName.trim();
    }

    public void changeEmail(String newEmail) {
        if (newEmail == null || newEmail.isBlank())
            throw new IllegalArgumentException("Email cannot be blank");

        this.email = normalizeEmail(newEmail);
    }

    public void changeRole(Role newRole) {
        if (newRole == null)
            throw new IllegalArgumentException("Role cannot be null");

        this.role = newRole;
    }

    public void changePassword(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isBlank())
            throw new IllegalArgumentException("Password cannot be blank");

        this.password = encryptedPassword;
    }

    public void changeTenant(Tenant newTenant) {
        if (newTenant == null)
            throw new IllegalArgumentException("Tenant cannot be null");

        this.tenant = newTenant;
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
       SETTERS CONTROLADOS
       ====================================================== */

    public void setName(String name) {
        changeName(name);
    }

    public void setEmail(String email) {
        changeEmail(email);
    }

    public void setRole(Role role) {
        changeRole(role);
    }

    public void setTenant(Tenant tenant) {
        changeTenant(tenant);
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    /**
     * Intended for testing or controlled persistence scenarios only.
     */
    public void setId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("ID already set");
        }
        this.id = id;
    }

    /* ======================================================
       EQUALS & HASHCODE (JPA SAFE)
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
       TO STRING
       ====================================================== */

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", tenant=" + (tenant != null ? tenant.getSchemaName() : null) +
                '}';
    }
}