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
import java.util.UUID;
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

        if (lead == null) {
            throw new IllegalArgumentException("Lead cannot be null");
        }

        if (newStatus == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        // 🔒 Evita duplicação consecutiva de status
        Optional<LeadStatusHistory> last =
                historyRepository.findFirstByLeadOrderByChangedAtDesc(lead);

        if (last.isPresent() && last.get().getStatus() == newStatus) {
            return last.get();
        }

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

        if (lead == null) {
            throw new IllegalArgumentException("Lead cannot be null");
        }

        return historyRepository.findByLeadOrderByChangedAtDesc(lead);
    }

    @Transactional(readOnly = true)
    public Optional<LeadStatusHistory> getLastStatus(Lead lead) {

        if (lead == null) {
            throw new IllegalArgumentException("Lead cannot be null");
        }

        return historyRepository.findFirstByLeadOrderByChangedAtDesc(lead);
    }

    @Transactional(readOnly = true)
    public List<LeadStatusHistory> getHistoryByStatus(LeadStatus status) {

        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        return historyRepository.findByStatus(status);
    }

    /* ======================================================
       READ (DTO SAFE FOR CONTROLLER)
       ====================================================== */

    @Transactional(readOnly = true)
    public List<LeadStatusHistoryResponse> getHistoryByLead(Lead lead) {

        if (lead == null) {
            throw new IllegalArgumentException("Lead cannot be null");
        }

        return historyRepository
                .findByLeadOrderByChangedAtDesc(lead)
                .stream()
                .map(history -> new LeadStatusHistoryResponse(
                        history.getId(),
                        history.getStatus(),
                        history.getChangedAt(),
                        history.getChangedBy() != null
                                ? history.getChangedBy().getEmail()
                                : "SYSTEM"
                ))
                .collect(Collectors.toList());
    }

    /* ======================================================
       GET BY ID
       ====================================================== */

    @Transactional(readOnly = true)
    public LeadStatusHistory getById(UUID historyId) {

        if (historyId == null) {
            throw new IllegalArgumentException("History id cannot be null");
        }

        return historyRepository.findById(historyId)
                .orElseThrow(() ->
                        new IllegalArgumentException("History not found")
                );
    }
}