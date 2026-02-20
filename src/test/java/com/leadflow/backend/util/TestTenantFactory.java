package com.leadflow.backend.util;

import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.multitenancy.service.TenantProvisioningService;
import org.springframework.stereotype.Component;

@Component
public class TestTenantFactory {

    private final TenantProvisioningService tenantProvisioningService;

    public TestTenantFactory(TenantProvisioningService tenantProvisioningService) {
        this.tenantProvisioningService = tenantProvisioningService;
    }

    /* ======================================================
       CREATE TENANT (SEM ATIVAR CONTEXTO)
       ====================================================== */

    /**
     * Cria um tenant (schema físico) e retorna o nome normalizado
     * do schema gerado.
     *
     * NÃO altera o TenantContext automaticamente.
     */
    public String createTenant(String tenantName) {

        if (tenantName == null || tenantName.isBlank()) {
            throw new IllegalArgumentException("Tenant name cannot be blank");
        }

        String schemaName = normalizeSchema(tenantName);

        // Provisionamento deve ocorrer fora de transação multi-tenant
        tenantProvisioningService.provisionTenant(tenantName);

        return schemaName;
    }

    /* ======================================================
       CREATE + ACTIVATE
       ====================================================== */

    /**
     * Cria o tenant e já ativa o schema no TenantContext.
     * Ideal para testes de isolamento.
     */
    public String createAndActivate(String tenantName) {

        String schema = createTenant(tenantName);

        TenantContext.setTenant(schema);

        return schema;
    }

    /* ======================================================
       CONTEXT CONTROL
       ====================================================== */

    public void setTenantContext(String schemaName) {

        if (schemaName == null || schemaName.isBlank()) {
            throw new IllegalArgumentException("Schema name cannot be blank");
        }

        TenantContext.setTenant(schemaName.trim().toLowerCase());
    }

    public void clear() {
        TenantContext.clear();
    }

    /* ======================================================
       INTERNAL
       ====================================================== */

    private String normalizeSchema(String name) {
        return name
                .trim()
                .toLowerCase()
                .replace(" ", "_");
    }
}