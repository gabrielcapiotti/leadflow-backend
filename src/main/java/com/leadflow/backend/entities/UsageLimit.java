package com.leadflow.backend.entities;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Controla consumo total de recursos do tenant.
 * Diferente de VendorUsage (que rastreia períodos mensais),
 * esta entidade mantém contadores consolidados vinculados ao plano.
 */
@Entity
@Table(name = "usage_limits", schema = "public")
public class UsageLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    @Column(name = "leads_used", nullable = false)
    private Integer leadsUsed = 0;

    @Column(name = "users_used", nullable = false)
    private Integer usersUsed = 0;

    @Column(name = "ai_executions_used", nullable = false)
    private Integer aiExecutionsUsed = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    public UsageLimit() {
        // Required by JPA
    }

    /* ======================================================
       Getters and setters
       ====================================================== */

    public Long getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public Integer getLeadsUsed() {
        return leadsUsed;
    }

    public void setLeadsUsed(Integer leadsUsed) {
        this.leadsUsed = leadsUsed;
    }

    public Integer getUsersUsed() {
        return usersUsed;
    }

    public void setUsersUsed(Integer usersUsed) {
        this.usersUsed = usersUsed;
    }

    public Integer getAiExecutionsUsed() {
        return aiExecutionsUsed;
    }

    public void setAiExecutionsUsed(Integer aiExecutionsUsed) {
        this.aiExecutionsUsed = aiExecutionsUsed;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }
}
