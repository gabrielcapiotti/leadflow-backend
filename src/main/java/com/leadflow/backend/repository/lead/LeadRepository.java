package com.leadflow.backend.repository.lead;

import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID> {

    /* ======================================================
       CONSULTAS ADMINISTRATIVAS (USO INTERNO DO TENANT)
       ====================================================== */

    @EntityGraph(attributePaths = {"user"})
    List<Lead> findByDeletedAtIsNull();

    @EntityGraph(attributePaths = {"user"})
    List<Lead> findByStatusAndDeletedAtIsNull(LeadStatus status);

    Optional<Lead> findByEmailAndDeletedAtIsNull(String email);

    long countByStatusAndDeletedAtIsNull(LeadStatus status);

    /* ======================================================
       ISOLAMENTO POR USUÁRIO (PROTEÇÃO CONTRA IDOR)
       ====================================================== */

    List<Lead> findByUserAndDeletedAtIsNull(User user);

    List<Lead> findByUserAndStatusAndDeletedAtIsNull(
            User user,
            LeadStatus status
    );

    Optional<Lead> findByIdAndUserAndDeletedAtIsNull(
            UUID id,
            User user
    );
}
