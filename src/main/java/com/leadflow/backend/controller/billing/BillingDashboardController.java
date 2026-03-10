package com.leadflow.backend.controller.billing;

import com.leadflow.backend.dto.billing.BillingDashboardDTO;
import com.leadflow.backend.dto.billing.SubscriptionDetailsDTO;
import com.leadflow.backend.entities.StripeEventLog;
import com.leadflow.backend.security.VendorContext;
import com.leadflow.backend.service.billing.BillingDashboardService;
import com.leadflow.backend.service.vendor.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
@Slf4j
public class BillingDashboardController {

    private final BillingDashboardService billingDashboardService;
    private final VendorContext vendorContext;
    private final SubscriptionService subscriptionService;

    /**
     * GET /api/v1/billing/dashboard/{tenantId}
     * Retorna dashboard completo de billing
     */
    @GetMapping("/dashboard/{tenantId}")
    @PreAuthorize("@securityService.isTenantOwner(#tenantId)")
    public ResponseEntity<BillingDashboardDTO> getDashboard(@PathVariable UUID tenantId) {
        log.info("Fetching billing dashboard for tenant: {}", tenantId);
        BillingDashboardDTO dashboard = billingDashboardService.getBillingDashboard(tenantId);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * GET /api/v1/billing/subscription/{tenantId}
     * Retorna detalhes da assinatura
     */
    @GetMapping("/subscription/{tenantId}")
    @PreAuthorize("@securityService.isTenantOwner(#tenantId)")
    public ResponseEntity<SubscriptionDetailsDTO> getSubscriptionDetails(@PathVariable UUID tenantId) {
        log.info("Fetching subscription details for tenant: {}", tenantId);
        SubscriptionDetailsDTO details = billingDashboardService.getSubscriptionDetails(tenantId);
        return ResponseEntity.ok(details);
    }

    /**
     * GET /api/v1/billing/events/{tenantId}
     * Retorna histórico de eventos Stripe processados
     */
    @GetMapping("/events/{tenantId}")
    @PreAuthorize("@securityService.isTenantOwner(#tenantId)")
    public ResponseEntity<List<StripeEventLog>> getEventHistory(
        @PathVariable UUID tenantId,
        @RequestParam(defaultValue = "20") int limit) {
        
        log.info("Fetching event history for tenant: {} (limit: {})", tenantId, limit);
        List<StripeEventLog> events = billingDashboardService.getEventHistory(tenantId, limit);
        return ResponseEntity.ok(events);
    }

    /**
     * GET /api/v1/billing/usage/{tenantId}
     * Retorna estatísticas de uso (quota)
     */
    @GetMapping("/usage/{tenantId}")
    @PreAuthorize("@securityService.isTenantOwner(#tenantId)")
    public ResponseEntity<BillingDashboardDTO.UsageStatisticsDTO> getUsageStatistics(@PathVariable UUID tenantId) {
        log.info("Fetching usage statistics for tenant: {}", tenantId);
        BillingDashboardDTO.UsageStatisticsDTO usage = billingDashboardService.getUsageStatistics(tenantId);
        return ResponseEntity.ok(usage);
    }

    /**
     * GET /api/v1/billing/health
     * Verifica saúde do sistema de billing (retries pendentes, etc)
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BillingHealthDTO> getHealth() {
        log.info("Fetching billing system health");
        
        boolean pendingRetries = billingDashboardService.hasPendingRetries();
        
        BillingHealthDTO health = BillingHealthDTO.builder()
            .status(pendingRetries ? "WARNING" : "OK")
            .hasPendingRetries(pendingRetries)
            .message(pendingRetries ? 
                "Sistema tem eventos pendentes de retry" : 
                "Todos os eventos processados com sucesso")
            .timestamp(java.time.LocalDateTime.now())
            .build();
        
        return ResponseEntity.ok(health);
    }

    /**
     * GET /api/v1/billing/subscription
     * Retorna apenas a subscription do tenant autenticado (sem path param)
     */
    @GetMapping("/subscription")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SubscriptionDetailsDTO> getMySubscription() {
        UUID tenantId = vendorContext.getCurrentVendorId();
        log.info("Fetching subscription for authenticated tenant: {}", tenantId);
        SubscriptionDetailsDTO details = billingDashboardService.getSubscriptionDetails(tenantId);
        return ResponseEntity.ok(details);
    }

    /**
     * GET /api/v1/billing/usage
     * Retorna estatísticas de uso do tenant autenticado (sem path param)
     */
    @GetMapping("/usage")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BillingDashboardDTO.UsageStatisticsDTO> getMyUsage() {
        UUID tenantId = vendorContext.getCurrentVendorId();
        log.info("Fetching usage statistics for authenticated tenant: {}", tenantId);
        BillingDashboardDTO.UsageStatisticsDTO usage = billingDashboardService.getUsageStatistics(tenantId);
        return ResponseEntity.ok(usage);
    }

    /**
     * POST /api/v1/billing/cancel
     * Cancels the subscription for the authenticated tenant
     */
    @PostMapping("/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelSubscription() {
        UUID tenantId = vendorContext.getCurrentVendorId();
        log.warn("Cancelling subscription for tenant: {}", tenantId);
        
        try {
            subscriptionService.cancelSubscription(tenantId);
            
            return ResponseEntity.ok(Map.of(
                "status", "subscription_cancelled",
                "tenantId", tenantId.toString(),
                "timestamp", java.time.LocalDateTime.now()
            ));
        } catch (Exception e) {
            log.error("Failed to cancel subscription for tenant: {}", tenantId, e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "cancellation_failed",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * DTO para health check
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class BillingHealthDTO {
        private String status;
        private Boolean hasPendingRetries;
        private String message;
        private java.time.LocalDateTime timestamp;
    }
}
