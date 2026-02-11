package com.leadflow.backend.repository.lead;

import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.lead.LeadStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeadStatusHistoryRepository extends JpaRepository<LeadStatusHistory, Long> {

    /* ==========================
       HISTÓRICO POR LEAD
       ========================== */

    // Histórico completo de um lead (mais recente primeiro)
    List<LeadStatusHistory> findByLeadOrderByChangedAtDesc(Lead lead);

    // Último status registrado de um lead
    Optional<LeadStatusHistory> findFirstByLeadOrderByChangedAtDesc(Lead lead);

    /* ==========================
       CONSULTAS POR STATUS
       ========================== */

    // Histórico de mudanças por status
    List<LeadStatusHistory> findByStatus(LeadStatus status);

    // Histórico de um lead filtrado por status
    List<LeadStatusHistory> findByLeadAndStatus(
            Lead lead,
            LeadStatus status
    );
}
