package com.leadflow.backend.service.lead;

import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.lead.LeadStatusHistory;
import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.repository.lead.LeadStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class LeadStatusHistoryService {

    private final LeadStatusHistoryRepository historyRepository;

    public LeadStatusHistoryService(
            LeadStatusHistoryRepository historyRepository
    ) {
        this.historyRepository = historyRepository;
    }

    /* ==========================
       CREATE / REGISTER
       ========================== */

    /**
     * Registra uma mudança de status no histórico.
     * Deve ser chamado APENAS após o Lead mudar de status.
     */
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

    /* ==========================
       READ
       ========================== */

    /**
     * Retorna todo o histórico de um lead
     * (do mais recente para o mais antigo)
     */
    @Transactional(readOnly = true)
    public List<LeadStatusHistory> getHistoryByLead(Lead lead) {
        return historyRepository.findByLeadOrderByChangedAtDesc(lead);
    }

    /**
     * Retorna o último status registrado de um lead
     */
    @Transactional(readOnly = true)
    public Optional<LeadStatusHistory> getLastStatus(Lead lead) {
        return historyRepository.findFirstByLeadOrderByChangedAtDesc(lead);
    }

    /**
     * Retorna todo o histórico por status
     */
    @Transactional(readOnly = true)
    public List<LeadStatusHistory> getHistoryByStatus(LeadStatus status) {
        return historyRepository.findByStatus(status);
    }
}
