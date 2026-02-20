package com.leadflow.backend.service.lead;

import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.lead.LeadStatusHistory;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.lead.LeadRepository;
import com.leadflow.backend.repository.lead.LeadStatusHistoryRepository;
import com.leadflow.backend.repository.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LeadService {

    private final LeadRepository leadRepository;
    private final LeadStatusHistoryRepository historyRepository;
    private final UserRepository userRepository;

    public LeadService(
            LeadRepository leadRepository,
            LeadStatusHistoryRepository historyRepository,
            UserRepository userRepository
    ) {
        this.leadRepository = leadRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
    }

    /* ======================================================
       RESOLVE USER (para controller)
       ====================================================== */

    @Transactional(readOnly = true)
    public User resolveUser(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }

        return userRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );
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
       LIST
       ====================================================== */

    @Transactional(readOnly = true)
    public List<Lead> listActiveLeads(User user) {

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        return leadRepository.findByUserAndDeletedAtIsNull(user);
    }

    /* ======================================================
       UPDATE STATUS
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
       SOFT DELETE
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

    /* ======================================================
       GET BY ID (ISOLADO)
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
}
