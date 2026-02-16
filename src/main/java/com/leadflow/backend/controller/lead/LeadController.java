package com.leadflow.backend.controller.lead;

import com.leadflow.backend.dto.lead.CreateLeadRequest;
import com.leadflow.backend.dto.lead.LeadResponse;
import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.service.lead.LeadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    /* ==========================
       HELPER - CONVERTE PRINCIPAL
       ========================== */

    private User buildUserFromPrincipal(UserDetails principal) {

        if (principal == null) {
            throw new RuntimeException("User not authenticated");
        }

        // Aqui usamos apenas o username (email)
        // O Service deve buscar o usuário real se necessário
        return new User(principal.getUsername(), null, null, null);
    }

    /* ==========================
       LIST
       ========================== */

    @GetMapping
    public ResponseEntity<List<LeadResponse>> list(
            @AuthenticationPrincipal UserDetails principal
    ) {

        User currentUser = buildUserFromPrincipal(principal);

        List<LeadResponse> response = leadService
                .listActiveLeads(currentUser)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    /* ==========================
       CREATE
       ========================== */

    @PostMapping
    public ResponseEntity<LeadResponse> create(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateLeadRequest request
    ) {

        User currentUser = buildUserFromPrincipal(principal);

        Lead lead = leadService.createLead(
                request.getName(),
                request.getEmail(),
                request.getPhone(),
                currentUser
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(lead));
    }

    /* ==========================
       GET BY ID
       ========================== */

    @GetMapping("/{id}")
    public ResponseEntity<LeadResponse> getById(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id
    ) {

        User currentUser = buildUserFromPrincipal(principal);

        Lead lead = leadService.getByIdForUser(id, currentUser);

        return ResponseEntity.ok(toResponse(lead));
    }

    /* ==========================
       UPDATE STATUS
       ========================== */

    @PatchMapping("/{id}/status")
    public ResponseEntity<LeadResponse> updateStatus(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestParam LeadStatus status
    ) {

        User currentUser = buildUserFromPrincipal(principal);

        Lead lead = leadService.updateStatus(id, status, currentUser);

        return ResponseEntity.ok(toResponse(lead));
    }

    /* ==========================
       DELETE
       ========================== */

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id
    ) {

        User currentUser = buildUserFromPrincipal(principal);

        leadService.softDelete(id, currentUser);

        return ResponseEntity.noContent().build();
    }

    /* ==========================
       MAPPER
       ========================== */

    private LeadResponse toResponse(Lead lead) {

        if (lead == null) {
            throw new RuntimeException("Lead cannot be null");
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
