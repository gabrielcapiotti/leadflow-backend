package com.leadflow.backend.repository.user;

import com.leadflow.backend.entities.user.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    /* ======================================================
       CONSULTA PRINCIPAL (RBAC / AUTH)
       ====================================================== */

    Optional<Role> findByNameIgnoreCase(String name);

    /* ======================================================
       VALIDAÇÃO / BOOTSTRAP
       ====================================================== */

    boolean existsByNameIgnoreCase(String name);

    void findByName(String string);
}
