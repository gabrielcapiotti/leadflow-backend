package com.leadflow.backend.repository.tenant;

import com.leadflow.backend.entities.Tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /* ======================================================
       READ (ACTIVE ONLY)
       ====================================================== */

    Optional<Tenant> findBySchemaNameIgnoreCaseAndDeletedAtIsNull(String schemaName);

    Optional<Tenant> findByNameIgnoreCaseAndDeletedAtIsNull(String name);

    Optional<Tenant> findByIdAndDeletedAtIsNull(UUID id);

    /* ======================================================
       EXISTS (ACTIVE ONLY)
       ====================================================== */

    boolean existsBySchemaNameIgnoreCaseAndDeletedAtIsNull(String schemaName);

    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

    /* ======================================================
       ADMIN (INCLUDE DELETED)
       ====================================================== */

    Optional<Tenant> findBySchemaNameIgnoreCase(String schemaName);

    boolean existsBySchemaNameIgnoreCase(String schemaName);
}