package com.leadflow.backend.controller.lead;

import com.leadflow.backend.dto.lead.CreateLeadRequest;
import com.leadflow.backend.dto.lead.LeadResponse;
import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.vendor.QuotaType;
import com.leadflow.backend.entities.vendor.SubscriptionAccessLevel;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.security.VendorContext;
import com.leadflow.backend.security.SubscriptionGuard;
import com.leadflow.backend.service.lead.LeadService;
import com.leadflow.backend.service.vendor.QuotaService;
import com.leadflow.backend.service.user.UserService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private final LeadService leadService;
    private final UserService userService;
    private final SubscriptionGuard subscriptionGuard;
        private final VendorContext vendorContext;
        private final QuotaService quotaService;

    public LeadController(
            LeadService leadService,
            UserService userService,
                        SubscriptionGuard subscriptionGuard,
                        VendorContext vendorContext,
                        QuotaService quotaService
    ) {
        this.leadService = leadService;
        this.userService = userService;
        this.subscriptionGuard = subscriptionGuard;
                this.vendorContext = vendorContext;
                this.quotaService = quotaService;
    }

    /* ====================================================== */
    /* CREATE                                                 */
    /* ====================================================== */

    @PostMapping
    public ResponseEntity<?> createLead(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateLeadRequest request
    ) {

        if (subscriptionGuard.resolveAccess() != SubscriptionAccessLevel.FULL) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error", "SUBSCRIPTION_READ_ONLY",
                            "message", "Assinatura não permite criar leads."
                    ));
        }

        try {
            quotaService.checkQuota(
                    vendorContext.getCurrentVendor().getId(),
                    QuotaType.ACTIVE_LEADS
            );
        } catch (IllegalStateException ex) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error", "QUOTA_EXCEEDED",
                            "message", ex.getMessage()
                    ));
        }

        User user = resolveAuthenticatedUser(principal);

        Lead lead = leadService.createLead(
                request.getName(),
                request.getEmail(),
                request.getPhone(),
                user
        );

        quotaService.increment(
                vendorContext.getCurrentVendor().getId(),
                QuotaType.ACTIVE_LEADS
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
    public ResponseEntity<?> updateLeadStatus(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id,
            @RequestParam LeadStatus status
    ) {

        if (subscriptionGuard.resolveAccess() != SubscriptionAccessLevel.FULL) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error", "SUBSCRIPTION_READ_ONLY",
                            "message", "Assinatura não permite editar leads."
                    ));
        }

        User user = resolveAuthenticatedUser(principal);

        Lead lead = leadService.updateStatus(id, status, user);

        return ResponseEntity.ok(new LeadResponse(lead));
    }

    /* ====================================================== */
    /* DELETE (SOFT DELETE)                                   */
    /* ====================================================== */

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLead(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id
    ) {

        if (subscriptionGuard.resolveAccess() != SubscriptionAccessLevel.FULL) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error", "SUBSCRIPTION_READ_ONLY",
                            "message", "Assinatura não permite editar leads."
                    ));
        }

        User user = resolveAuthenticatedUser(principal);

        leadService.softDelete(id, user);

        return ResponseEntity.noContent().build();
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