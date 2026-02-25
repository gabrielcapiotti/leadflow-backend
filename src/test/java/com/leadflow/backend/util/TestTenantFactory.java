package com.leadflow.backend.util;

import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.multitenancy.service.TenantProvisioningService;
import com.leadflow.backend.repository.tenant.TenantRepository;

import org.springframework.stereotype.Component;

@Component
public class TestTenantFactory {

    private final TenantProvisioningService tenantProvisioningService;
    private final TenantRepository tenantRepository;

    public TestTenantFactory(
            TenantProvisioningService tenantProvisioningService,
            TenantRepository tenantRepository
    ) {
        this.tenantProvisioningService = tenantProvisioningService;
        this.tenantRepository = tenantRepository;
    }

    /* ======================================================
       CREATE TENANT (SEM ATIVAR CONTEXTO)
       ====================================================== */

    /**
     * Cria o tenant fisicamente (schema + registro no public)
     * e retorna a entidade persistida.
     *
     * NÃO altera o TenantContext automaticamente.
     *
     * ⚠ NÃO é transacional.
     */
    public Tenant createTenant(String tenantName) {

        if (tenantName == null || tenantName.isBlank()) {
            throw new IllegalArgumentException("Tenant name cannot be blank");
        }

        String normalizedName = tenantName.trim();

        // Provisiona schema + executa flyway
        tenantProvisioningService.provisionTenant(normalizedName);

        // Recupera tenant persistido respeitando soft delete
        return tenantRepository
                .findByNameIgnoreCaseAndDeletedAtIsNull(normalizedName)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Tenant was not persisted after provisioning: " + normalizedName
                        )
                );
    }

    /* ======================================================
       CREATE + ACTIVATE
       ====================================================== */

    public Tenant createAndActivate(String tenantName) {

        Tenant tenant = createTenant(tenantName);

        TenantContext.setTenant(tenant.getSchemaName());

        return tenant;
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
}