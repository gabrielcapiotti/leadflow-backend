package com.leadflow.backend.service.billing;

import com.leadflow.backend.dto.billing.BillingDashboardDTO;
import com.leadflow.backend.dto.billing.SubscriptionDetailsDTO;
import com.leadflow.backend.entities.Plan;
import com.leadflow.backend.entities.Subscription;
import com.leadflow.backend.entities.StripeEventLog;
import com.leadflow.backend.entities.UsageLimit;
import com.leadflow.backend.repository.SubscriptionRepository;
import com.leadflow.backend.repository.StripeEventLogRepository;
import com.leadflow.backend.repository.UsageLimitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BillingDashboardService {

    private final SubscriptionRepository subscriptionRepository;
    private final StripeEventLogRepository eventLogRepository;
    private final StripeWebhookProcessingService processingService;
    private final UsageLimitRepository usageLimitRepository;

    /**
     * Retorna dashboard de billing para um tenant
     */
    public BillingDashboardDTO getBillingDashboard(UUID tenantId) {
        Subscription subscription = subscriptionRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Subscription not found for tenant: " + tenantId
            ));

        SubscriptionDetailsDTO subscriptionDetails = SubscriptionDetailsDTO.fromEntity(subscription);
        
        // Obter estatísticas de eventos
        var eventStats = processingService.getEventStatistics();
        double successRate = calculateSuccessRate(eventStats);

        BillingDashboardDTO.EventStatisticsDTO eventStatistics = BillingDashboardDTO.EventStatisticsDTO.builder()
            .totalProcessed(eventStats.getTotalProcessed())
            .totalFailed(eventStats.getTotalFailed())
            .totalPending(eventStats.getTotalPending())
            .totalRetryPending(eventStats.getTotalRetryPending())
            .successRate(successRate)
            .build();

        // Status atual
        boolean isActive = subscription.getStatus() == Subscription.SubscriptionStatus.ACTIVE;
        String currentStatus = getStatusDisplay(subscription.getStatus());
        String nextAction = getNextAction(subscription);

        return BillingDashboardDTO.builder()
            .subscription(subscriptionDetails)
            .eventStatistics(eventStatistics)
            .hasActiveSubscription(isActive)
            .currentStatus(currentStatus)
            .nextAction(nextAction)
            .build();
    }

    /**
     * Retorna detalhes completos da subscription para um tenant
     */
    public SubscriptionDetailsDTO getSubscriptionDetails(UUID tenantId) {
        Subscription subscription = subscriptionRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Subscription not found for tenant: " + tenantId
            ));

        return SubscriptionDetailsDTO.fromEntity(subscription);
    }

    /**
     * Retorna histórico de eventos processados
     */
    public java.util.List<StripeEventLog> getEventHistory(UUID tenantId, int limit) {
        // Retorna últimos N eventos (padrão: 20)
        return eventLogRepository.findByStatuses(
            java.util.Arrays.asList(
                StripeEventLog.EventProcessingStatus.SUCCESS,
                StripeEventLog.EventProcessingStatus.FAILED,
                StripeEventLog.EventProcessingStatus.RETRY_PENDING
            )
        ).stream()
        .limit(limit)
        .toList();
    }

    /**
     * Verifica se há eventos pendentes de retry
     */
    public boolean hasPendingRetries() {
        long pendingCount = eventLogRepository
            .countByStatus(StripeEventLog.EventProcessingStatus.RETRY_PENDING);
        return pendingCount > 0;
    }

    /**
     * Retorna dados de uso da quota para um tenant.
     * Compara uso atual contra limite do plano e calcula percentual.
     */
    public BillingDashboardDTO.UsageStatisticsDTO getUsageStatistics(UUID tenantId) {
        try {
            // Get usage limit record for tenant
            UsageLimit usageLimit = usageLimitRepository.findByTenantId(tenantId)
                .orElse(null);

            if (usageLimit == null || usageLimit.getPlan() == null) {
                log.warn("No usage limit found for tenant: {}", tenantId);
                return createDefaultUsageStats();
            }

            Plan plan = usageLimit.getPlan();
            long leadsUsed = usageLimit.getLeadsUsed().longValue();
            long leadsLimit = plan.getMaxLeads().longValue();

            // Calculate usage percentage
            double usagePercentage = (leadsLimit > 0) ? (leadsUsed * 100.0) / leadsLimit : 0.0;

            // Determine status based on usage
            String usageStatus = getUsageStatus(usagePercentage);

            log.debug("Usage statistics for tenant {}: {}/{} leads ({}%)",
                tenantId, leadsUsed, leadsLimit, String.format("%.1f", usagePercentage));

            return BillingDashboardDTO.UsageStatisticsDTO.builder()
                .leadsCreated(leadsUsed)
                .leadsLimit(leadsLimit)
                .usagePercentage(usagePercentage)
                .usageStatus(usageStatus)
                .build();

        } catch (Exception e) {
            log.error("Error calculating usage statistics for tenant: {}", tenantId, e);
            return createDefaultUsageStats();
        }
    }

    /**
     * Determina o status de uso baseado no percentual
     */
    private String getUsageStatus(double usagePercentage) {
        if (usagePercentage >= 100.0) {
            return "EXCEEDED";
        } else if (usagePercentage >= 90.0) {
            return "CRITICAL";
        } else if (usagePercentage >= 75.0) {
            return "WARNING";
        } else {
            return "OK";
        }
    }

    /**
     * Retorna estatísticas de uso padrão quando dados não estão disponíveis
     */
    private BillingDashboardDTO.UsageStatisticsDTO createDefaultUsageStats() {
        return BillingDashboardDTO.UsageStatisticsDTO.builder()
            .leadsCreated(0L)
            .leadsLimit(0L)
            .usagePercentage(0.0)
            .usageStatus("UNKNOWN")
            .build();
    }

    private double calculateSuccessRate(StripeWebhookProcessingService.EventStatistics stats) {
        long total = stats.getTotalProcessed() + stats.getTotalFailed();
        if (total == 0) {
            return 100.0;
        }
        return (stats.getTotalProcessed() * 100.0) / total;
    }

    private String getStatusDisplay(Subscription.SubscriptionStatus status) {
        return switch (status) {
            case ACTIVE -> "✅ Ativa";
            case PAST_DUE -> "⚠️ Vencida";
            case CANCELLED -> "❌ Cancelada";
            case INCOMPLETE -> "⏳ Incompleta";
        };
    }

    private String getNextAction(Subscription subscription) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = subscription.getExpiresAt();

        if (subscription.getStatus() == Subscription.SubscriptionStatus.CANCELLED) {
            return "Renovar assinatura para reativar acesso";
        }

        if (subscription.getStatus() == Subscription.SubscriptionStatus.PAST_DUE) {
            return "Atualizar método de pagamento";
        }

        long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(now, expiresAt);
        
        if (daysUntil <= 0) {
            return "Assinatura expirou - Renovar agora";
        } else if (daysUntil <= 7) {
            return "Assinatura expira em " + daysUntil + " dias";
        } else {
            return "Assinatura ativa até " + expiresAt.toLocalDate();
        }
    }
}
