package com.leadflow.backend.repository.lead;

import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID> {

    /* ======================================================
       CONSULTAS ADMINISTRATIVAS (TENANT CONTEXT)
       ====================================================== */

    List<Lead> findByDeletedAtIsNull();

    List<Lead> findByStatusAndDeletedAtIsNull(LeadStatus status);

    Optional<Lead> findByEmailAndDeletedAtIsNull(String email);

    long countByStatusAndDeletedAtIsNull(LeadStatus status);

    /* ======================================================
       ISOLAMENTO POR USER ID (PROTEÇÃO CONTRA IDOR)
       ====================================================== */

    List<Lead> findByUserIdAndDeletedAtIsNull(UUID userId);

    List<Lead> findByUserIdAndStatusAndDeletedAtIsNull(
            UUID userId,
            LeadStatus status
    );

    Optional<Lead> findByIdAndUserIdAndDeletedAtIsNull(
            UUID id,
            UUID userId
    );
}