package com.leadflow.backend.entities.vendor;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vendors")
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nomeVendedor;

    @Column(nullable = false)
    private String whatsappVendedor;

    private String nomeEmpresa;
    private String logoUrl;

    @Column(nullable = false)
    private String corDestaque = "#FF7A00";

    @Column(columnDefinition = "TEXT")
    private String mensagemBoasVindas;

    @Column(nullable = false, unique = true)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", nullable = false)
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.TRIAL;

    @Column(name = "subscription_started_at")
    private Instant subscriptionStartedAt;

    @Column(name = "subscription_expires_at")
    private Instant subscriptionExpiresAt;

    @Column(name = "next_billing_at")
    private Instant nextBillingAt;

    @Column(name = "external_subscription_id")
    private String externalSubscriptionId;

    @Column(name = "external_customer_id")
    private String externalCustomerId;

    @Column(name = "last_payment_at")
    private Instant lastPaymentAt;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private boolean emailInvalid = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    private String schemaName;

    @Column(name = "name", nullable = true)
    private String name;

    public Vendor() {}

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ======================
    // GETTERS
    // ======================

    public UUID getId() {
        return id;
    }

    public String getNomeVendedor() {
        return nomeVendedor;
    }

    public String getWhatsappVendedor() {
        return whatsappVendedor;
    }

    public String getNomeEmpresa() {
        return nomeEmpresa;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getCorDestaque() {
        return corDestaque;
    }

    public String getMensagemBoasVindas() {
        return mensagemBoasVindas;
    }

    public String getSlug() {
        return slug;
    }

    public SubscriptionStatus getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public Instant getSubscriptionStartedAt() {
        return subscriptionStartedAt;
    }

    public Instant getSubscriptionExpiresAt() {
        return subscriptionExpiresAt;
    }

    public Instant getNextBillingAt() {
        return nextBillingAt;
    }

    public String getExternalSubscriptionId() {
        return externalSubscriptionId;
    }

    public String getExternalCustomerId() {
        return externalCustomerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastPaymentAt() {
        return lastPaymentAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isEmailInvalid() {
        return emailInvalid;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getName() {
        return name;
    }

    // ======================
    // SETTERS
    // ======================

    public void setId(UUID id) {
        this.id = id;
    }

    public void setNomeVendedor(String nomeVendedor) {
        this.nomeVendedor = nomeVendedor;
    }

    public void setWhatsappVendedor(String whatsappVendedor) {
        this.whatsappVendedor = whatsappVendedor;
    }

    public void setNomeEmpresa(String nomeEmpresa) {
        this.nomeEmpresa = nomeEmpresa;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public void setCorDestaque(String corDestaque) {
        this.corDestaque = corDestaque;
    }

    public void setMensagemBoasVindas(String mensagemBoasVindas) {
        this.mensagemBoasVindas = mensagemBoasVindas;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setSubscriptionStatus(SubscriptionStatus subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public void setSubscriptionStartedAt(Instant subscriptionStartedAt) {
        this.subscriptionStartedAt = subscriptionStartedAt;
    }

    public void setSubscriptionExpiresAt(Instant subscriptionExpiresAt) {
        this.subscriptionExpiresAt = subscriptionExpiresAt;
    }

    public void setNextBillingAt(Instant nextBillingAt) {
        this.nextBillingAt = nextBillingAt;
    }

    public void setExternalSubscriptionId(String externalSubscriptionId) {
        this.externalSubscriptionId = externalSubscriptionId;
    }

    public void setExternalCustomerId(String externalCustomerId) {
        this.externalCustomerId = externalCustomerId;
    }

    public void setLastPaymentAt(Instant lastPaymentAt) {
        this.lastPaymentAt = lastPaymentAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setEmailInvalid(boolean emailInvalid) {
        this.emailInvalid = emailInvalid;
    }

    public void setStatusAssinatura(String statusAssinatura) {
        this.subscriptionStatus = SubscriptionStatus.valueOf(statusAssinatura.toUpperCase());
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public void setName(String name) {
        this.name = name;
    }
}