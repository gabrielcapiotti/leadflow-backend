package com.leadflow.backend.health;

import com.leadflow.backend.webhook.repository.FailedWebhookRepository;
import com.leadflow.backend.webhook.entity.FailedWebhookEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for webhook processing system.
 * 
 * Monitors webhook retry queue health:
 * - Number of pending webhooks
 * - Processing rate
 * - Age of oldest pending webhook
 *
 * @author LeadFlow Team
 * @version 1.0
 */
@Slf4j
@Component
public class WebhookHealthIndicator implements HealthIndicator {

    @Autowired(required = false)
    private FailedWebhookRepository failedWebhookRepository;

    @Value("${webhook.health.max-pending:100}")
    private int maxPendingWarning;

    @Value("${webhook.health.max-age-hours:24}")
    private int maxAgeHours;

    @Override
    public Health health() {
        if (failedWebhookRepository == null) {
            return Health.unknown()
                    .withDetail("service", "Webhook")
                    .withDetail("message", "Repository not configured")
                    .build();
        }

        try {
            // Get webhook queue statistics
            long pendingCount = failedWebhookRepository.countByStatus(
                FailedWebhookEvent.WebhookStatus.PENDING
            );
            long failedCount = failedWebhookRepository.countByStatus(
                FailedWebhookEvent.WebhookStatus.FAILED_PERMANENT
            );
            long successCount = failedWebhookRepository.countByStatus(
                FailedWebhookEvent.WebhookStatus.SUCCEEDED
            );

            Health.Builder builder = Health.up();
            builder.withDetail("service", "Webhook");
            builder.withDetail("pending_count", pendingCount);
            builder.withDetail("succeeded_count", successCount);
            builder.withDetail("failed_count", failedCount);

            // Check if queue is growing
            if (pendingCount > maxPendingWarning) {
                log.warn("Webhook queue warning: {} pending (max: {})", pendingCount, maxPendingWarning);
                builder.status("DEGRADED");
                builder.withDetail("warning", "Pending webhook queue exceeds threshold");
                builder.withDetail("threshold", maxPendingWarning);
            }

            // Check for permanently failed webhooks
            if (failedCount > 10) {
                log.warn("High number of permanently failed webhooks: {}", failedCount);
                builder.withDetail("alert", "Many permanently failed webhooks require investigation");
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Webhook health check failed: {}", e.getMessage(), e);
            return Health.down()
                    .withDetail("service", "Webhook")
                    .withDetail("error", "Health check error: " + e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}
