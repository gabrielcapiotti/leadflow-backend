package com.leadflow.backend.repository.user;

import com.leadflow.backend.entities.user.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /* ======================================================
       CONSULTA PRINCIPAL (RBAC / AUTH)
       ====================================================== */

    Optional<Role> findByNameIgnoreCase(String name);

    /* ======================================================
       VALIDAÇÃO / BOOTSTRAP
       ====================================================== */

    boolean existsByNameIgnoreCase(String name);
}
