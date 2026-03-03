package com.leadflow.backend.controller;

import com.leadflow.backend.dto.vendor.StageConversionResponse;
import com.leadflow.backend.dto.vendor.StageTimeMetricsResponse;
import com.leadflow.backend.dto.vendor.UpdateStageRequest;
import com.leadflow.backend.dto.vendor.VendorLeadMetricsResponse;
import com.leadflow.backend.entities.vendor.VendorLeadAlert;
import com.leadflow.backend.entities.vendor.VendorLeadConversation;
import com.leadflow.backend.entities.vendor.VendorLead;
import com.leadflow.backend.repository.VendorLeadAlertRepository;
import com.leadflow.backend.service.vendor.ResumoService;
import com.leadflow.backend.service.vendor.VendorLeadService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/vendor-leads")
public class VendorLeadController {

    private final VendorLeadService service;
    private final ResumoService resumoService;
    private final VendorLeadAlertRepository alertRepository;

    public VendorLeadController(VendorLeadService service,
                                ResumoService resumoService,
                                VendorLeadAlertRepository alertRepository) {
        this.service = service;
        this.resumoService = resumoService;
        this.alertRepository = alertRepository;
    }

    @PutMapping("/{id}/stage")
    public ResponseEntity<?> updateStage(
            @PathVariable UUID id,
            @RequestBody UpdateStageRequest request) {

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

        return ResponseEntity.ok(service.getMetricsForCurrentVendor());
    }

    @GetMapping("/ranking")
    public ResponseEntity<List<VendorLead>> getRanking() {

        return ResponseEntity.ok(
            service.getRankingForCurrentVendor()
        );
    }

    @PutMapping("/{id}/owner")
    public ResponseEntity<VendorLead> assignOwner(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                service.assignOwner(id)
        );
    }

    @GetMapping("/metrics/stage-time")
        public ResponseEntity<StageTimeMetricsResponse> getStageTimeMetrics() {

        return ResponseEntity.ok(
            service.calculateAverageStageTimeForCurrentVendor()
        );
    }

    @GetMapping("/metrics/conversion")
        public ResponseEntity<StageConversionResponse> getConversionMetrics() {

        return ResponseEntity.ok(
            service.calculateConversionRatesForCurrentVendor()
        );
    }

    @GetMapping("/{id}/conversation")
    public ResponseEntity<List<VendorLeadConversation>> getConversation(
            @PathVariable UUID id) {

        service.getLeadForCurrentVendor(id);

        return ResponseEntity.ok(service.getConversation(id));
    }

    @GetMapping("/{id}/alerts")
    public ResponseEntity<List<VendorLeadAlert>> getOpenAlerts(
            @PathVariable UUID id) {

        service.getLeadForCurrentVendor(id);

        return ResponseEntity.ok(
                alertRepository.findByVendorLeadIdAndResolvidoFalseOrderByCreatedAtDesc(id)
        );
    }

    @PutMapping("/{id}/resumo")
    public ResponseEntity<String> gerarResumo(@PathVariable UUID id) {

        String resumo = resumoService.gerarResumo(id);

        return ResponseEntity.ok(resumo);
    }
}