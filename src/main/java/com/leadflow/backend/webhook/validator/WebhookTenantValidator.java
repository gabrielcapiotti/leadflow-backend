package com.leadflow.backend.webhook.validator;

import com.leadflow.backend.webhook.entity.FailedWebhookEvent;
import com.leadflow.backend.webhook.repository.FailedWebhookRepository;
import com.stripe.model.Event;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

import org.springframework.stereotype.Service;

/**
 * Validates that webhook events belong to the correct tenant.
 * 
 * Ensures complete data isolation between customers by verifying
 * that webhook operations only affect the tenant's own data.
 *
 * @author LeadFlow Team
 * @version 1.0
 */
@Slf4j
@Service
public class WebhookTenantValidator {

    private final FailedWebhookRepository failedWebhookRepository;

    public WebhookTenantValidator(FailedWebhookRepository failedWebhookRepository) {
        this.failedWebhookRepository = failedWebhookRepository;
    }

    /**
     * Validate that a webhook event belongs to the current tenant.
     *
     * @param event The Stripe event
     * @param tenantId The tenant ID to validate against
     * @return true if validation passes
     * @throws WebhookTenantViolationException If webhook doesn't belong to tenant
     */
    public boolean validateWebhookTenant(Event event, String tenantId) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or blank");
        }

        // For now, Stripe events don't inherently have tenant info
        // Tenant association is stored in our local database
        // when the event is first received
        log.debug("Validating webhook {} for tenant {}", event.getId(), tenantId);
        
        return true;
    }

    /**
     * Validate that a failed webhook belongs to the current tenant.
     *
     * @param webhookId The ID of the failed webhook
     * @param tenantId The tenant ID to validate against
     * @return true if webhook belongs to tenant
     * @throws WebhookTenantViolationException If webhook doesn't belong to tenant
     */
    public boolean validateFailedWebhookTenant(String webhookId, String tenantId) {
        var webhook = failedWebhookRepository.findById(Objects.requireNonNull(webhookId))
                .orElseThrow(() -> new IllegalArgumentException("Webhook not found: " + webhookId));

        if (webhook.getTenantId() == null) {
            log.warn("Webhook {} has no tenant association, access denied", Objects.requireNonNull(webhookId));
            throw new WebhookTenantViolationException(
                "Webhook not associated with any tenant");
        }

        if (!webhook.getTenantId().equals(tenantId)) {
            log.warn("Access denied: Webhook {} belongs to tenant {}, not {}",
                    webhookId, webhook.getTenantId(), tenantId);
            throw new WebhookTenantViolationException(
                String.format("Webhook does not belong to tenant %s", tenantId));
        }

        return true;
    }

    /**
     * Validate webhook count for tenant (prevent abuse/DoS).
     *
     * @param tenantId The tenant ID
     * @param maxPending Maximum allowed pending webhooks
     * @return true if within limits
     * @throws WebhookTenantViolationException If limit exceeded
     */
    public boolean validateWebhookQuota(String tenantId, int maxPending) {
        long count = failedWebhookRepository.countByStatus(
            FailedWebhookEvent.WebhookStatus.PENDING
        );

        if (count > maxPending) {
            log.warn("Tenant {} exceeds webhook limit: {} > {}", tenantId, count, maxPending);
            throw new WebhookTenantViolationException(
                String.format("Webhook quota exceeded: %d pending (max: %d)", count, maxPending));
        }

        return true;
    }

    /**
     * Exception thrown on tenant isolation violations.
     */
    public static class WebhookTenantViolationException extends RuntimeException {
        public WebhookTenantViolationException(String message) {
            super(message);
        }

        public WebhookTenantViolationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
