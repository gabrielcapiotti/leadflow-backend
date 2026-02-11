package com.leadflow.backend.repository.user;

import com.leadflow.backend.entities.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /* ==========================
       AUTENTICAÇÃO
       ========================== */

    // Login seguro (apenas usuários ativos)
    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    /* ==========================
       VALIDAÇÃO DE CADASTRO
       ========================== */

    // Verifica existência de email ativo
    boolean existsByEmailAndDeletedAtIsNull(String email);

    /* ==========================
       CONSULTAS ADMIN / INTERNAS
       ========================== */

    // Busca por email (inclui usuários soft-deleted)
    Optional<User> findByEmail(String email);

    // Lista todos os usuários ativos
    List<User> findByDeletedAtIsNull();
}
