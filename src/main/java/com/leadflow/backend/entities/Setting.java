package com.leadflow.backend.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.leadflow.backend.entities.user.User;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
    name = "settings",
    indexes = {
        @Index(name = "idx_settings_user", columnList = "user_id")
    }
)
public class Setting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ==========================
       RELACIONAMENTO
       ========================== */

    /**
     * Pode ser null:
     * - settings globais
     * - bootstrap
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "user_id",
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

    /**
     * Construtor protegido exigido pelo JPA
     */
    protected Setting() {
    }

    /**
     * Construtor de domínio
     */
    public Setting(
            User user,
            String vendorName,
            String whatsapp,
            String companyName,
            String logo,
            String welcomeMessage
    ) {
        this.user = user;
        this.vendorName = vendorName;
        this.whatsapp = whatsapp;
        this.companyName = companyName;
        this.logo = logo;
        this.welcomeMessage = welcomeMessage;
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

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
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

    /* ==========================
       EQUALS & HASHCODE
       ========================== */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Setting)) return false;
        Setting setting = (Setting) o;
        return Objects.equals(id, setting.id);
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
        return "Setting{" +
               "id=" + id +
               ", vendorName='" + vendorName + '\'' +
               ", companyName='" + companyName + '\'' +
               '}';
    }
}
