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
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /* ======================================================
       LIST
       ====================================================== */

    @GetMapping
    public ResponseEntity<List<LeadResponse>> list(
            @AuthenticationPrincipal UserDetails principal
    ) {

        User user = resolveUser(principal);

        List<LeadResponse> response = leadService
                .listActiveLeads(user)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    /* ======================================================
       CREATE
       ====================================================== */

    @PostMapping
    public ResponseEntity<LeadResponse> create(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateLeadRequest request
    ) {

        User user = resolveUser(principal);

        Lead lead = leadService.createLead(
                request.getName(),
                request.getEmail(),
                request.getPhone(),
                user
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(lead));
    }

    /* ======================================================
       GET BY ID
       ====================================================== */

    @GetMapping("/{id}")
    public ResponseEntity<LeadResponse> getById(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id
    ) {

        User user = resolveUser(principal);

        Lead lead = leadService.getByIdForUser(id, user);

        return ResponseEntity.ok(toResponse(lead));
    }

    /* ======================================================
       UPDATE STATUS
       ====================================================== */

    @PatchMapping("/{id}/status")
    public ResponseEntity<LeadResponse> updateStatus(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestParam LeadStatus status
    ) {

        User user = resolveUser(principal);

        Lead lead = leadService.updateStatus(id, status, user);

        return ResponseEntity.ok(toResponse(lead));
    }

    /* ======================================================
       DELETE (SOFT)
       ====================================================== */

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id
    ) {

        User user = resolveUser(principal);

        leadService.softDelete(id, user);

        return ResponseEntity.noContent().build();
    }

    /* ======================================================
       INTERNAL
       ====================================================== */

    private User resolveUser(UserDetails principal) {

        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException(
                    "User not authenticated"
            );
        }

        return userService.getActiveByEmail(principal.getUsername());
    }

    /* ======================================================
       MAPPER
       ====================================================== */

    private LeadResponse toResponse(Lead lead) {

        if (lead == null) {
            throw new IllegalArgumentException("Lead cannot be null");
        }

        return new LeadResponse(
                lead.getId(),
                lead.getName(),
                lead.getEmail(),
                lead.getPhone(),
                lead.getStatus(),
                lead.getCreatedAt()
        );
    }
}
