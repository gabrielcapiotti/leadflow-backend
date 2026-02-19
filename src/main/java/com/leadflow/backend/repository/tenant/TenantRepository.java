package com.leadflow.backend.repository.tenant;

import com.leadflow.backend.entities.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findBySchemaName(String schemaName);

    Optional<Tenant> findByNameIgnoreCase(String name);

    boolean existsBySchemaName(String schemaName);

    Object findByName(String string);
}
