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

    @Column(nullable = false)
    private String statusAssinatura = "pendente";

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

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

    public String getStatusAssinatura() {
        return statusAssinatura;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
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

    public void setStatusAssinatura(String statusAssinatura) {
        this.statusAssinatura = statusAssinatura;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}