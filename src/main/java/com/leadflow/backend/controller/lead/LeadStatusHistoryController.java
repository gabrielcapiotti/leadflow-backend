package com.leadflow.backend.controller.lead;

import com.leadflow.backend.dto.lead.LeadStatusHistoryResponse;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.lead.LeadStatusHistory;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.service.lead.LeadService;
import com.leadflow.backend.service.lead.LeadStatusHistoryService;
import com.leadflow.backend.service.user.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leads")
public class LeadStatusHistoryController {

    private final LeadService leadService;
    private final LeadStatusHistoryService historyService;
    private final UserService userService;

    public LeadStatusHistoryController(
            LeadService leadService,
            LeadStatusHistoryService historyService,
            UserService userService
    ) {
        this.leadService = leadService;
        this.historyService = historyService;
        this.userService = userService;
    }

    /* ======================================================
       HISTORY BY LEAD (ISOLATED BY USER)
       ====================================================== */

    @GetMapping("/{leadId}/history")
    public ResponseEntity<List<LeadStatusHistoryResponse>> getHistory(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID leadId
    ) {

        User user = resolveUser(principal);

        Lead lead = leadService.getByIdForUser(leadId, user.getId());

        if (lead == null) {
            return ResponseEntity.notFound().build();
        }

        List<LeadStatusHistoryResponse> response =
                historyService.getHistoryByLead(lead);

        return ResponseEntity.ok(response);
    }

    /* ======================================================
       HISTORY BY HISTORY ID (ISOLATED BY USER)
       ====================================================== */

    @GetMapping("/history/{historyId}")
    public ResponseEntity<LeadStatusHistoryResponse> getHistoryById(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID historyId
    ) {

        User user = resolveUser(principal);

        LeadStatusHistory history = historyService.getById(historyId);

        if (history == null) {
            return ResponseEntity.notFound().build();
        }

        if (history.getLead() == null ||
            !history.getLead().getUserId().equals(user.getId())) {

            // Evita vazamento de existência entre usuários
            return ResponseEntity.notFound().build();
        }

        LeadStatusHistoryResponse response =
                new LeadStatusHistoryResponse(
                        history.getId(),
                        history.getStatus(),
                        history.getChangedAt(),
                        history.getChangedBy() != null
                                ? history.getChangedBy().getEmail()
                                : "SYSTEM"
                );

        return ResponseEntity.ok(response);
    }

    /* ======================================================
       INTERNAL
       ====================================================== */

    private User resolveUser(UserDetails principal) {

        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Authentication required"
            );
        }

        User user = userService.getActiveByEmail(principal.getUsername());

        if (user == null) {
            throw new AccessDeniedException("Authenticated user not found");
        }

        return user;
    }
}