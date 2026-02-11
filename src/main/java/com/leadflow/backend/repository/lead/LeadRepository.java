package com.leadflow.backend.repository.lead;

import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    /* ==========================
       CONSULTAS GERAIS (ADMIN / INTERNO)
       ========================== */

    // Todos os leads ativos (uso administrativo)
    List<Lead> findByDeletedAtIsNull();

    // Leads ativos por status (uso administrativo)
    List<Lead> findByStatusAndDeletedAtIsNull(LeadStatus status);

    // Busca por email (único lógico)
    Optional<Lead> findByEmailAndDeletedAtIsNull(String email);

    // Contagem por status
    long countByStatusAndDeletedAtIsNull(LeadStatus status);

    /* ==========================
       ISOLAMENTO POR USUÁRIO (CORE)
       ========================== */

    // Leads ativos de um usuário
    List<Lead> findByUserAndDeletedAtIsNull(User user);

    // Leads ativos de um usuário por status
    List<Lead> findByUserAndStatusAndDeletedAtIsNull(
            User user,
            LeadStatus status
    );

    // Busca segura por ID (evita IDOR)
    Optional<Lead> findByIdAndUserAndDeletedAtIsNull(
            Long id,
            User user
    );
}
