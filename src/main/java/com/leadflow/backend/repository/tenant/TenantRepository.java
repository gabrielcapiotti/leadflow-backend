package com.leadflow.backend.repository.tenant;

import com.leadflow.backend.entities.Tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /* ======================================================
       ACTIVE TENANTS (Soft Delete Applied)
       ====================================================== */

    /**
     * Busca tenant ativo pelo schemaName.
     */
    Optional<Tenant> findBySchemaNameIgnoreCaseAndDeletedAtIsNull(String schemaName);

    /**
     * Busca tenant ativo pelo nome.
     */
    Optional<Tenant> findByNameIgnoreCaseAndDeletedAtIsNull(String name);

    /**
     * Busca tenant ativo por ID.
     */
    Optional<Tenant> findByIdAndDeletedAtIsNull(UUID id);

    /**
     * Verifica existência de tenant ativo pelo schemaName.
     */
    boolean existsBySchemaNameIgnoreCaseAndDeletedAtIsNull(String schemaName);

    /**
     * Verifica existência de tenant ativo pelo nome.
     */
    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

    /* ======================================================
       INCLUDE SOFT-DELETED (ADMIN / SYSTEM)
       ====================================================== */

    /**
     * Busca tenant (ativo ou deletado) pelo schemaName.
     * Usado para validações administrativas.
     */
    Optional<Tenant> findBySchemaNameIgnoreCase(String schemaName);

    /**
     * Verifica existência de tenant (ativo ou deletado).
     */
    boolean existsBySchemaNameIgnoreCase(String schemaName);
}