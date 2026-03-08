package com.leadflow.backend.repository.user;

import com.leadflow.backend.entities.user.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /* ======================================================
       AUTH / RBAC
       ====================================================== */

    Optional<Role> findByNameIgnoreCase(String name);
    Optional<Role> findByName(String name);

    /* ======================================================
       VALIDAÇÃO / BOOTSTRAP
       ====================================================== */

    boolean existsByNameIgnoreCase(String name);
    boolean existsByName(String name);
}