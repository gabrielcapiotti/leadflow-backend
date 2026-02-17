package com.leadflow.backend.service.lead;

import com.leadflow.backend.dto.lead.LeadStatusHistoryResponse;
import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.lead.LeadStatusHistory;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.lead.LeadStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LeadStatusHistoryService {

    private final LeadStatusHistoryRepository historyRepository;

    public LeadStatusHistoryService(
            LeadStatusHistoryRepository historyRepository
    ) {
        this.historyRepository = historyRepository;
    }

    /* ======================================================
       REGISTER STATUS CHANGE
       ====================================================== */

    @Transactional
    public LeadStatusHistory registerStatusChange(
            Lead lead,
            LeadStatus newStatus,
            User changedBy
    ) {

        LeadStatusHistory history = new LeadStatusHistory(
                lead,
                newStatus,
                changedBy
        );

        return historyRepository.save(history);
    }

    /* ======================================================
       READ (ENTITY)
       ====================================================== */

    @Transactional(readOnly = true)
    public List<LeadStatusHistory> getHistoryEntitiesByLead(Lead lead) {
        return historyRepository.findByLeadOrderByChangedAtDesc(lead);
    }

    @Transactional(readOnly = true)
    public Optional<LeadStatusHistory> getLastStatus(Lead lead) {
        return historyRepository.findFirstByLeadOrderByChangedAtDesc(lead);
    }

    @Transactional(readOnly = true)
    public List<LeadStatusHistory> getHistoryByStatus(LeadStatus status) {
        return historyRepository.findByStatus(status);
    }

    /* ======================================================
       READ (DTO SAFE FOR CONTROLLER)
       ====================================================== */

    @Transactional(readOnly = true)
    public List<LeadStatusHistoryResponse> getHistoryByLead(Lead lead) {

        return historyRepository
                .findByLeadOrderByChangedAtDesc(lead)
                .stream()
                .map(history -> new LeadStatusHistoryResponse(
                        history.getId(),
                        history.getStatus(),
                        history.getChangedAt(),
                        history.getChangedBy().getEmail()
                ))
                .collect(Collectors.toList());
    }
}
