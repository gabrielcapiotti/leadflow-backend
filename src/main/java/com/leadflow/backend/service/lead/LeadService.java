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
@Transactional
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

    /* ====================================================== */
    /* RESOLVE USER                                           */
    /* ====================================================== */

    @Transactional(readOnly = true)
    public User resolveUser(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }

        return userRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull(email.trim())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );
    }

    /* ====================================================== */
    /* CREATE                                                 */
    /* ====================================================== */

    public Lead createLead(
            String name,
            String email,
            String phone,
            User createdBy
    ) {

        requireUser(createdBy);
        requireEmail(email);

        String normalizedEmail = email.trim().toLowerCase();

        boolean exists = leadRepository
                .existsByUserIdAndEmailIgnoreCaseAndDeletedAtIsNull(
                        createdBy.getId(),
                        normalizedEmail
                );

        if (exists) {
            throw new IllegalArgumentException("Email already in use");
        }

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

    /* ====================================================== */
    /* LIST                                                   */
    /* ====================================================== */

    @Transactional(readOnly = true)
    public List<Lead> listActiveLeads(User user) {

        requireUser(user);

        return leadRepository
                .findByUserIdAndDeletedAtIsNull(user.getId());
    }

    /* ====================================================== */
    /* UPDATE STATUS                                          */
    /* ====================================================== */

    public Lead updateStatus(
            UUID leadId,
            LeadStatus newStatus,
            User user
    ) {

        requireUser(user);

        if (leadId == null) {
            throw new IllegalArgumentException("LeadId cannot be null");
        }

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

    /* ====================================================== */
    /* SOFT DELETE                                            */
    /* ====================================================== */

    public void softDelete(UUID leadId, User user) {

        requireUser(user);

        if (leadId == null) {
            throw new IllegalArgumentException("LeadId cannot be null");
        }

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

    /* ====================================================== */
    /* GET BY ID                                              */
    /* ====================================================== */

    @Transactional(readOnly = true)
    public Lead getByIdForUser(UUID leadId, UUID userId) {

        if (leadId == null) {
            throw new IllegalArgumentException("LeadId cannot be null");
        }

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

    /* ====================================================== */
    /* PRIVATE GUARDS                                         */
    /* ====================================================== */

    private void requireUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
    }

    private void requireEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
    }
}