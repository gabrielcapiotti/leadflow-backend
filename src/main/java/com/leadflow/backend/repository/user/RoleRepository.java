package com.leadflow.backend.repository.user;

import com.leadflow.backend.entities.user.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    /* ==========================
       CONSULTAS PRINCIPAIS
       ========================== */

    // Busca role por nome (uso em AuthService, RBAC)
    Optional<Role> findByName(String name);

    /* ==========================
       VALIDAÇÕES / ADMIN
       ========================== */

    // Verifica existência de role (útil para bootstrap / migrations)
    boolean existsByName(String name);
}
