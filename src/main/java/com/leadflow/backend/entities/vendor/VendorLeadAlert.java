package com.leadflow.backend.entities.vendor;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vendor_lead_alerts")
public class VendorLeadAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID vendorLeadId;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensagem;

    @Column(nullable = false)
    private boolean resolvido = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public VendorLeadAlert() {}

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getVendorLeadId() { return vendorLeadId; }
    public String getTipo() { return tipo; }
    public String getMensagem() { return mensagem; }
    public boolean isResolvido() { return resolvido; }
    public Instant getCreatedAt() { return createdAt; }

    public void setVendorLeadId(UUID vendorLeadId) {
        this.vendorLeadId = vendorLeadId;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public void setResolvido(boolean resolvido) {
        this.resolvido = resolvido;
    }
}
