package com.leadflow.backend.controller;

import com.leadflow.backend.dto.vendor.CreateLeadRequest;
import com.leadflow.backend.dto.vendor.StageConversionResponse;
import com.leadflow.backend.dto.vendor.StageTimeMetricsResponse;
import com.leadflow.backend.dto.vendor.UpdateStageRequest;
import com.leadflow.backend.dto.vendor.VendorLeadMetricsResponse;
import com.leadflow.backend.entities.vendor.SubscriptionAccessLevel;
import com.leadflow.backend.entities.vendor.VendorLeadAlert;
import com.leadflow.backend.entities.vendor.VendorLeadConversation;
import com.leadflow.backend.entities.vendor.VendorLead;
import com.leadflow.backend.repository.VendorLeadAlertRepository;
import com.leadflow.backend.security.SubscriptionGuard;
import com.leadflow.backend.service.vendor.ResumoService;
import com.leadflow.backend.service.vendor.VendorLeadService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/vendor-leads")
@PreAuthorize("@subscriptionGuard.isActive()")
public class VendorLeadController {

    private final VendorLeadService service;
    private final ResumoService resumoService;
    private final VendorLeadAlertRepository alertRepository;
    private final SubscriptionGuard subscriptionGuard;

    public VendorLeadController(VendorLeadService service,
                                ResumoService resumoService,
                                VendorLeadAlertRepository alertRepository,
                                SubscriptionGuard subscriptionGuard) {
        this.service = service;
        this.resumoService = resumoService;
        this.alertRepository = alertRepository;
        this.subscriptionGuard = subscriptionGuard;
    }

    @PostMapping("/leads")
    public ResponseEntity<?> createLead(
            @Valid @RequestBody CreateLeadRequest request) {

        if (subscriptionGuard.resolveAccess() != SubscriptionAccessLevel.FULL) {
            return ResponseEntity.status(403).body(
                Map.of(
                    "error", "SUBSCRIPTION_READ_ONLY",
                    "message", "Assinatura não permite criar leads."
                )
            );
        }

        service.create(request);

        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Page<VendorLead>> list(Pageable pageable) {

        subscriptionGuard.assertActive();

        return ResponseEntity.ok(service.listForCurrentVendor(pageable));
    }

    @PutMapping("/{id}/stage")
    public ResponseEntity<?> updateStage(
            @PathVariable UUID id,
            @RequestBody UpdateStageRequest request) {

        if (subscriptionGuard.resolveAccess() != SubscriptionAccessLevel.FULL) {
            return ResponseEntity.status(403).body(
                Map.of(
                    "error", "SUBSCRIPTION_READ_ONLY",
                    "message", "Assinatura não permite editar leads."
                )
            );
        }

        try {
            VendorLead updated =
                    service.updateStage(id, request.getStage());

            return ResponseEntity.ok(updated);

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/metrics")
    public ResponseEntity<VendorLeadMetricsResponse> getMetrics() {

        subscriptionGuard.assertActive();

        return ResponseEntity.ok(service.getMetricsForCurrentVendor());
    }

    @GetMapping("/ranking")
    public ResponseEntity<List<VendorLead>> getRanking() {

        subscriptionGuard.assertActive();

        return ResponseEntity.ok(
            service.getRankingForCurrentVendor()
        );
    }

    @PutMapping("/{id}/owner")
    public ResponseEntity<VendorLead> assignOwner(
            @PathVariable UUID id) {

        if (subscriptionGuard.resolveAccess() != SubscriptionAccessLevel.FULL) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(
                service.assignOwner(id)
        );
    }

    @GetMapping("/metrics/stage-time")
        public ResponseEntity<StageTimeMetricsResponse> getStageTimeMetrics() {

        subscriptionGuard.assertActive();

        return ResponseEntity.ok(
            service.calculateAverageStageTimeForCurrentVendor()
        );
    }

    @GetMapping("/metrics/conversion")
        public ResponseEntity<StageConversionResponse> getConversionMetrics() {

        subscriptionGuard.assertActive();

        return ResponseEntity.ok(
            service.calculateConversionRatesForCurrentVendor()
        );
    }

    @GetMapping("/{id}/conversation")
    public ResponseEntity<List<VendorLeadConversation>> getConversation(
            @PathVariable UUID id) {

        subscriptionGuard.assertActive();

        service.getLeadForCurrentVendor(id);

        return ResponseEntity.ok(service.getConversation(id));
    }

    @GetMapping("/{id}/alerts")
    public ResponseEntity<List<VendorLeadAlert>> getOpenAlerts(
            @PathVariable UUID id) {

        subscriptionGuard.assertActive();

        service.getLeadForCurrentVendor(id);

        return ResponseEntity.ok(
                alertRepository.findByVendorLeadIdAndResolvidoFalseOrderByCreatedAtDesc(id)
        );
    }

    @PutMapping("/{id}/resumo")
    public ResponseEntity<String> gerarResumo(@PathVariable UUID id) {

        if (subscriptionGuard.resolveAccess() != SubscriptionAccessLevel.FULL) {
            return ResponseEntity.status(403).body("Assinatura não permite editar leads.");
        }

        String resumo = resumoService.gerarResumo(id);

        return ResponseEntity.ok(resumo);
    }
}