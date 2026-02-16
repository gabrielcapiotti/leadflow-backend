package com.leadflow.backend.service.lead;

import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.lead.LeadStatusHistory;
import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.repository.lead.LeadRepository;
import com.leadflow.backend.repository.lead.LeadStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeadService {

    private final LeadRepository leadRepository;
    private final LeadStatusHistoryRepository historyRepository;

    public LeadService(
            LeadRepository leadRepository,
            LeadStatusHistoryRepository historyRepository
    ) {
        this.leadRepository = leadRepository;
        this.historyRepository = historyRepository;
    }

    /* ==========================
       CREATE
       ========================== */

    @Transactional
    public Lead createLead(
            String name,
            String email,
            String phone,
            User createdBy
    ) {

        if (createdBy == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        // 🔥 VALIDA DUPLICIDADE
        boolean emailExists = leadRepository
                .findByUserAndDeletedAtIsNull(createdBy)
                .stream()
                .anyMatch(l -> l.getEmail().equalsIgnoreCase(email));

        if (emailExists) {
            throw new IllegalArgumentException("Email already in use");
        }

        Lead lead = new Lead(name, email, phone);
        lead.setUser(createdBy);

        lead = leadRepository.save(lead);

        historyRepository.save(
                new LeadStatusHistory(lead, LeadStatus.NEW, createdBy)
        );

        return lead;
    }

    /* ==========================
       READ (ISOLADO POR USUÁRIO)
       ========================== */

    @Transactional(readOnly = true)
    public List<Lead> listActiveLeads(User user) {
        return leadRepository.findByUserAndDeletedAtIsNull(user);
    }

    /* ==========================
       READ BY ID (ISOLADO)
       ========================== */

    @Transactional(readOnly = true)
    public Lead getByIdForUser(Long leadId, User user) {
        return leadRepository.findByIdAndUserAndDeletedAtIsNull(leadId, user)
                .orElseThrow(() ->
                        new IllegalArgumentException("Lead not found")
                );
    }

    /* ==========================
       UPDATE STATUS (ISOLADO)
       ========================== */

    @Transactional
    public Lead updateStatus(
            Long leadId,
            LeadStatus newStatus,
            User changedBy
    ) {

        Lead lead = leadRepository.findByIdAndUserAndDeletedAtIsNull(
                    leadId,
                    changedBy
            )
            .orElseThrow(() ->
                    new IllegalArgumentException("Lead not found or already deleted")
            );

        LeadStatus oldStatus = lead.getStatus();

        if (!oldStatus.canTransitionTo(newStatus)) {
            throw new IllegalArgumentException("Invalid status transition");
        }

        if (oldStatus == newStatus) {
            return lead;
        }

        lead.changeStatus(newStatus);

        if (shouldSaveHistory(oldStatus, newStatus)) {
            historyRepository.save(
                    new LeadStatusHistory(lead, newStatus, changedBy)
            );
        }

        return lead;
    }

    private boolean shouldSaveHistory(LeadStatus oldStatus, LeadStatus newStatus) {
        boolean shouldSave = oldStatus != newStatus;
        System.out.println("shouldSaveHistory: oldStatus=" + oldStatus + ", newStatus=" + newStatus + ", shouldSave=" + shouldSave);
        return shouldSave;
    }

    /* ==========================
       DELETE (SOFT, ISOLADO)
       ========================== */

    @Transactional
    public void softDelete(Long leadId, User user) {

        Lead lead = leadRepository.findByIdAndUserAndDeletedAtIsNull(
                        leadId,
                        user
                )
                .orElseThrow(() ->
                        new IllegalArgumentException("Lead not found")
                );

        lead.setDeletedAt(LocalDateTime.now());
        leadRepository.save(lead);
    }
}
