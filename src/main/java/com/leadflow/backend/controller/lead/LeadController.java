package com.leadflow.backend.controller.lead;

import com.leadflow.backend.dto.lead.CreateLeadRequest;
import com.leadflow.backend.dto.lead.LeadResponse;
import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.service.lead.LeadService;
import com.leadflow.backend.service.user.UserService;

import org.springframework.http.ResponseEntity;
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

    /* ======================================================
       CREATE
       ====================================================== */

    @PostMapping
    public ResponseEntity<LeadResponse> createLead(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody CreateLeadRequest request
    ) {

        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.getActiveByEmail(principal.getUsername());

        Lead lead = leadService.createLead(
                request.getName(),
                request.getEmail(),
                request.getPhone(),
                user
        );

        return ResponseEntity
                .status(201)
                .body(new LeadResponse(lead));
    }

    /* ======================================================
       LIST
       ====================================================== */

    @GetMapping
    public ResponseEntity<List<LeadResponse>> listLeads(
            @AuthenticationPrincipal UserDetails principal
    ) {

        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.getActiveByEmail(principal.getUsername());

        List<Lead> leads = leadService.listActiveLeads(user);

        List<LeadResponse> response = leads
                .stream()
                .map(LeadResponse::new)
                .toList();

        return ResponseEntity.ok(response);
    }

    /* ======================================================
       UPDATE STATUS
       ====================================================== */

    @PatchMapping("/{id}/status")
    public ResponseEntity<LeadResponse> updateLeadStatus(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id,
            @RequestParam LeadStatus status
    ) {

        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.getActiveByEmail(principal.getUsername());

        Lead lead = leadService.updateStatus(id, status, user);

        return ResponseEntity.ok(new LeadResponse(lead));
    }

    /* ======================================================
       DELETE (ISOLADO POR USUÁRIO)
       ====================================================== */

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLead(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id
    ) {

        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.getActiveByEmail(principal.getUsername());

        leadService.softDelete(id, user);

        return ResponseEntity.noContent().build();
    }
}
