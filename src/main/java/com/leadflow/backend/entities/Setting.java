package com.leadflow.backend.entities;

import com.leadflow.backend.entities.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "settings",
    indexes = {
        @Index(name = "idx_settings_user", columnList = "user_id")
    }
)
public class Setting {

    /* ==========================
       ID
       ========================== */

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    /* ==========================
       RELACIONAMENTO
       ========================== */

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_settings_user")
    )
    private User user;

    /* ==========================
       CAMPOS
       ========================== */

    @Column(name = "vendor_name", nullable = false, length = 100)
    private String vendorName;

    @Column(nullable = false, length = 15)
    private String whatsapp;

    @Column(name = "company_name", length = 100)
    private String companyName;

    @Column(columnDefinition = "TEXT")
    private String logo;

    @Column(name = "welcome_message", columnDefinition = "TEXT")
    private String welcomeMessage;

    /* ==========================
       AUDITORIA
       ========================== */

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

    protected Setting() {
        // JPA
    }

    public Setting(
            User user,
            String vendorName,
            String whatsapp,
            String companyName,
            String logo,
            String welcomeMessage
    ) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        validateVendorName(vendorName);
        validateWhatsapp(whatsapp);

        this.user = user;
        this.vendorName = vendorName.trim();
        this.whatsapp = whatsapp.trim();
        this.companyName = normalize(companyName);
        this.logo = normalize(logo);
        this.welcomeMessage = normalize(welcomeMessage);
    }

    /* ==========================
       GETTERS
       ========================== */

    public UUID getId() { return id; }

    public User getUser() { return user; }

    public String getVendorName() { return vendorName; }

    public String getWhatsapp() { return whatsapp; }

    public String getCompanyName() { return companyName; }

    public String getLogo() { return logo; }

    public String getWelcomeMessage() { return welcomeMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }

    /* ==========================
       REGRAS DE DOMÍNIO
       ========================== */

    public void update(
            String vendorName,
            String whatsapp,
            String companyName,
            String logo,
            String welcomeMessage
    ) {
        ensureNotDeleted();

        validateVendorName(vendorName);
        validateWhatsapp(whatsapp);

        this.vendorName = vendorName.trim();
        this.whatsapp = whatsapp.trim();
        this.companyName = normalize(companyName);
        this.logo = normalize(logo);
        this.welcomeMessage = normalize(welcomeMessage);
    }

    public void softDelete() {
        ensureNotDeleted();
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deletedAt = null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    private void ensureNotDeleted() {
        if (isDeleted()) {
            throw new IllegalStateException("Cannot modify deleted setting");
        }
    }

    private void validateVendorName(String vendorName) {
        if (vendorName == null || vendorName.isBlank()) {
            throw new IllegalArgumentException("Vendor name cannot be blank");
        }
    }

    private void validateWhatsapp(String whatsapp) {
        if (whatsapp == null || whatsapp.isBlank()) {
            throw new IllegalArgumentException("Whatsapp cannot be blank");
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    /* ==========================
       EQUALS & HASHCODE
       ========================== */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Setting other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
