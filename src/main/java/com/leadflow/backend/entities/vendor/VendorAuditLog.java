package com.leadflow.backend.entities.vendor;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vendor_audit_logs")
public class VendorAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID vendorId;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String acao;

    @Column(nullable = false)
    private UUID entidadeId;

    @Column(columnDefinition = "TEXT")
    private String detalhes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public VendorAuditLog() {}

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getVendorId() { return vendorId; }
    public String getUserEmail() { return userEmail; }
    public String getAcao() { return acao; }
    public UUID getEntidadeId() { return entidadeId; }
    public String getDetalhes() { return detalhes; }
    public Instant getCreatedAt() { return createdAt; }

    public void setVendorId(UUID vendorId) { this.vendorId = vendorId; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public void setAcao(String acao) { this.acao = acao; }
    public void setEntidadeId(UUID entidadeId) { this.entidadeId = entidadeId; }
    public void setDetalhes(String detalhes) { this.detalhes = detalhes; }
}
