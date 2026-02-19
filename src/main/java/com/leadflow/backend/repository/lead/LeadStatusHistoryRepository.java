package com.leadflow.backend.repository.lead;

import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.lead.LeadStatusHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeadStatusHistoryRepository extends JpaRepository<LeadStatusHistory, UUID> {

    /* ======================================================
       HISTÓRICO POR LEAD
       ====================================================== */

    @EntityGraph(attributePaths = {"lead"})
    List<LeadStatusHistory> findByLeadOrderByChangedAtDesc(Lead lead);

    @EntityGraph(attributePaths = {"lead"})
    Optional<LeadStatusHistory> findFirstByLeadOrderByChangedAtDesc(Lead lead);

    /* ======================================================
       CONSULTAS POR STATUS
       ====================================================== */

    List<LeadStatusHistory> findByStatus(LeadStatus status);

    List<LeadStatusHistory> findByLeadAndStatus(
            Lead lead,
            LeadStatus status
    );
}
