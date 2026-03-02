package com.leadflow.backend.controller.lead;

import com.leadflow.backend.dto.lead.CreateLeadRequest;
import com.leadflow.backend.dto.lead.LeadResponse;
import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.service.lead.LeadService;
import com.leadflow.backend.service.user.UserService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private final LeadService leadService;
    private final UserService userService;

    public LeadController(
            LeadService leadService,
            UserService userService
    ) {
        this.leadService = leadService;
        this.userService = userService;
    }

    /* ====================================================== */
    /* CREATE                                                 */
    /* ====================================================== */

    @PostMapping
    public ResponseEntity<LeadResponse> createLead(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateLeadRequest request
    ) {

        User user = resolveAuthenticatedUser(principal);

        Lead lead = leadService.createLead(
                request.getName(),
                request.getEmail(),
                request.getPhone(),
                user
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new LeadResponse(lead));
    }

    /* ====================================================== */
    /* LIST                                                   */
    /* ====================================================== */

    @GetMapping
    public ResponseEntity<List<LeadResponse>> listLeads(
            @AuthenticationPrincipal UserDetails principal
    ) {

        User user = resolveAuthenticatedUser(principal);

        List<LeadResponse> response = leadService
                .listActiveLeads(user)
                .stream()
                .map(LeadResponse::new)
                .toList();

        return ResponseEntity.ok(response);
    }

    /* ====================================================== */
    /* UPDATE STATUS                                          */
    /* ====================================================== */

    @PatchMapping("/{id}/status")
    public ResponseEntity<LeadResponse> updateLeadStatus(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id,
            @RequestParam LeadStatus status
    ) {

        User user = resolveAuthenticatedUser(principal);

        Lead lead = leadService.updateStatus(id, status, user);

        return ResponseEntity.ok(new LeadResponse(lead));
    }

    /* ====================================================== */
    /* DELETE (SOFT DELETE)                                   */
    /* ====================================================== */

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLead(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id
    ) {

        User user = resolveAuthenticatedUser(principal);

        leadService.softDelete(id, user);
    }

    /* ====================================================== */
    /* INTERNAL                                               */
    /* ====================================================== */

    /**
     * Resolve authenticated domain User from Spring Security principal.
     * Throws AccessDeniedException if authentication is invalid.
     */
    private User resolveAuthenticatedUser(UserDetails principal) {

        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }

        User user = userService.getActiveByEmail(principal.getUsername());

        if (user == null) {
            throw new AccessDeniedException("Authenticated user not found");
        }

        return user;
    }
}