package com.leadflow.backend.repository.lead;

import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.repository.tenant.TenantRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
public class TenantRepositoryTestHelper {

    private final TenantRepository tenantRepository;

    public TenantRepositoryTestHelper(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /* ======================================================
       DELETE ALL (PUBLIC SCHEMA)
       ====================================================== */

    @Transactional(transactionManager = "publicTransactionManager")
    public void deleteAll() {
        tenantRepository.deleteAll();
    }

    /* ======================================================
       SAVE + FLUSH (PUBLIC SCHEMA)
       ====================================================== */

    @Transactional(transactionManager = "publicTransactionManager")
    public Tenant saveAndFlush(Tenant tenant) {
        return tenantRepository.saveAndFlush(Objects.requireNonNull(tenant));
    }
}