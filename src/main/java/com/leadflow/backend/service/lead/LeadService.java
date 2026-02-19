package com.leadflow.backend.service.lead;

import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.lead.LeadStatusHistory;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.lead.LeadRepository;
import com.leadflow.backend.repository.lead.LeadStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

    /* ======================================================
       CREATE
       ====================================================== */

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

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }

        boolean emailExists = leadRepository
                .findByUserAndDeletedAtIsNull(createdBy)
                .stream()
                .anyMatch(l ->
                        l.getEmail() != null &&
                        l.getEmail().equalsIgnoreCase(email)
                );

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

    /* ======================================================
       LIST (ISOLADO POR USUÁRIO)
       ====================================================== */

    @Transactional(readOnly = true)
    public List<Lead> listActiveLeads(User user) {

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        return leadRepository.findByUserAndDeletedAtIsNull(user);
    }

    /* ======================================================
       READ BY ID (ISOLADO)
       ====================================================== */

    @Transactional(readOnly = true)
    public Lead getByIdForUser(UUID leadId, User user) {

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        return leadRepository
                .findByIdAndUserAndDeletedAtIsNull(leadId, user)
                .orElseThrow(() ->
                        new IllegalArgumentException("Lead not found or already deleted")
                );
    }

    /* ======================================================
       UPDATE STATUS (ISOLADO)
       ====================================================== */

    @Transactional
    public Lead updateStatus(
            UUID leadId,
            LeadStatus newStatus,
            User user
    ) {

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (newStatus == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        Lead lead = leadRepository
                .findByIdAndUserAndDeletedAtIsNull(leadId, user)
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

        historyRepository.save(
                new LeadStatusHistory(lead, newStatus, user)
        );

        return lead;
    }

    /* ======================================================
       SOFT DELETE (ISOLADO)
       ====================================================== */

    @Transactional
    public void softDelete(UUID leadId, User user) {

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        Lead lead = leadRepository
                .findByIdAndUserAndDeletedAtIsNull(leadId, user)
                .orElseThrow(() ->
                        new IllegalArgumentException("Lead not found or already deleted")
                );

        lead.softDelete();

        leadRepository.save(lead);
    }

    public Lead getById(UUID id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getById'");
    }
}
