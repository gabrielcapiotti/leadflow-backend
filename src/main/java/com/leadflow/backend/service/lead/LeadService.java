package com.leadflow.backend.service.lead;

import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.lead.LeadStatusHistory;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.lead.LeadRepository;
import com.leadflow.backend.repository.lead.LeadStatusHistoryRepository;
import com.leadflow.backend.repository.user.UserRepository;
import com.leadflow.backend.security.VendorContext;
import com.leadflow.backend.service.vendor.SubscriptionService;
import com.leadflow.backend.service.vendor.UsageService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class LeadService {

    private final LeadRepository leadRepository;
    private final LeadStatusHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final VendorContext vendorContext;
    private final UsageService usageService;
    private final SubscriptionService subscriptionService;

    public LeadService(
            LeadRepository leadRepository,
            LeadStatusHistoryRepository historyRepository,
            UserRepository userRepository,
            VendorContext vendorContext,
            UsageService usageService,
            SubscriptionService subscriptionService
    ) {
        this.leadRepository = leadRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.vendorContext = vendorContext;
        this.usageService = usageService;
        this.subscriptionService = subscriptionService;
    }

    /* ======================================================
       RESOLVE USER
       ====================================================== */

    @Transactional(readOnly = true)
    public User resolveUser(String email) {

        String normalized = normalizeEmail(email);

        return userRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull(normalized)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );
    }

    /* ======================================================
       CREATE
       ====================================================== */

    public Lead createLead(
            String name,
            String email,
            String phone,
            User createdBy
    ) {

        requireUser(createdBy);

        String normalizedEmail = normalizeEmail(email);

        boolean exists = leadRepository
                .existsByUserIdAndEmailIgnoreCaseAndDeletedAtIsNull(
                        createdBy.getId(),
                        normalizedEmail
                );

        if (exists) {
            throw new IllegalArgumentException("Email already in use");
        }

        UUID tenantId = vendorContext.getCurrentVendorId();
        
        // Validate subscriber has active subscription before creating lead
        subscriptionService.validateActiveSubscription(tenantId);
        
        usageService.consumeLead(tenantId);

        Lead lead = new Lead(
                createdBy.getId(),
                name,
                normalizedEmail,
                phone
        );

        leadRepository.save(lead);

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

        requireUser(user);

        return leadRepository
                .findByUserIdAndDeletedAtIsNull(user.getId());
    }

    /* ======================================================
       UPDATE STATUS
       ====================================================== */

    public Lead updateStatus(
            UUID leadId,
            LeadStatus newStatus,
            User user
    ) {

        requireUser(user);
        requireLeadId(leadId);

        if (newStatus == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        Lead lead = leadRepository
                .findByIdAndUserIdAndDeletedAtIsNull(
                        leadId,
                        user.getId()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException("Lead not found or already deleted")
                );

        if (lead.getStatus() == newStatus) {
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

    public void softDelete(UUID leadId, User user) {

        requireUser(user);
        requireLeadId(leadId);

        Lead lead = leadRepository
                .findByIdAndUserIdAndDeletedAtIsNull(
                        leadId,
                        user.getId()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException("Lead not found or already deleted")
                );

        lead.softDelete();
    }

    /* ======================================================
       GET BY ID
       ====================================================== */

    @Transactional(readOnly = true)
    public Lead getByIdForUser(UUID leadId, UUID userId) {

        requireLeadId(leadId);

        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }

        return leadRepository
                .findByIdAndUserIdAndDeletedAtIsNull(
                        leadId,
                        userId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException("Lead not found or already deleted")
                );
    }

    /* ======================================================
       PRIVATE GUARDS
       ====================================================== */

    private void requireUser(User user) {

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (user.getId() == null) {
            throw new IllegalArgumentException("User must have an id");
        }
    }

    private void requireLeadId(UUID leadId) {

        if (leadId == null) {
            throw new IllegalArgumentException("LeadId cannot be null");
        }
    }

    private String normalizeEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }

        return email.trim().toLowerCase();
    }
}