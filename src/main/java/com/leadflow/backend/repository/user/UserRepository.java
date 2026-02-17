package com.leadflow.backend.repository.user;

import com.leadflow.backend.entities.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /* ======================================================
       AUTENTICAÇÃO (USUÁRIOS ATIVOS)
       ====================================================== */

    @EntityGraph(attributePaths = {"role"})
    Optional<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    /* ======================================================
       VALIDAÇÃO DE CADASTRO
       ====================================================== */

    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    /* ======================================================
       CONSULTAS ADMINISTRATIVAS
       ====================================================== */

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByEmail(String email);

    Page<User> findByDeletedAtIsNull(Pageable pageable);

    boolean existsByEmailAndDeletedAtIsNull(String string);

    Object findByEmailAndDeletedAtIsNull(String string);
}
