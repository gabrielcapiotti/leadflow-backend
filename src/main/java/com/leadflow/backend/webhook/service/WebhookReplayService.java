package com.leadflow.backend.webhook.service;

import com.leadflow.backend.service.billing.StripeWebhookProcessor;
import com.leadflow.backend.webhook.entity.FailedWebhookEvent;
import com.leadflow.backend.webhook.repository.FailedWebhookRepository;
import com.stripe.model.Event;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Service for replaying failed webhook events.
 * Implements exponential backoff retry strategy.
 * 
 * Retry Schedule:
 * - 1st attempt: Immediate
 * - 2nd attempt: 1 minute
 * - 3rd attempt: 5 minutes
 * - 4th attempt: 30 minutes
 * - 5th attempt: 2 hours
 * 
 * @author LeadFlow Team
 * @version 1.0
 */
@Slf4j
@Service
public class WebhookReplayService {

    private final FailedWebhookRepository failedWebhookRepository;
    private final StripeWebhookProcessor webhookProcessor;

    @Value("${webhook.replay.max-retries:5}")
    private int maxRetries;

    @Value("${webhook.replay.batch-size:10}")
    private int batchSize;

    public WebhookReplayService(
            FailedWebhookRepository failedWebhookRepository,
            StripeWebhookProcessor webhookProcessor) {
        this.failedWebhookRepository = failedWebhookRepository;
        this.webhookProcessor = webhookProcessor;
    }

    /**
     * Store a failed webhook event for later replay.
     *
     * @param stripeEventId Unique Stripe event ID
     * @param eventType Type of event
     * @param eventData Raw webhook payload
     * @param failureReason Why it failed
     */
    @Transactional
    public void storeFailedWebhook(
            String stripeEventId,
            String eventType,
            String eventData,
            String failureReason) {

        // Check if already exists to prevent duplicates
        if (failedWebhookRepository.existsByStripeEventId(stripeEventId)) {
            log.debug("Webhook {} already stored, skipping duplicate", stripeEventId);
            return;
        }

        FailedWebhookEvent event = FailedWebhookEvent.builder()
                .stripeEventId(stripeEventId)
                .eventType(eventType)
                .eventData(eventData)
                .failureReason(failureReason)
                .retryCount(0)
                .maxRetries(maxRetries)
                .status(FailedWebhookEvent.WebhookStatus.PENDING)
                .nextRetryAt(Instant.now())
                .originalReceivedAt(Instant.now())
                .build();

        if (event != null) {
            failedWebhookRepository.save(event);
        }
        log.info("Stored failed webhook event: {} (type: {})", stripeEventId, eventType);
    }

    /**
     * Scheduled task to replay failed webhooks.
     * Runs every 30 seconds and processes up to batch-size pending webhooks.
     */
    @Scheduled(fixedRateString = "${webhook.replay.check-interval:30000}")
    @Transactional
    public void replayFailedWebhooks() {
        log.debug("Starting webhook replay check");

        Instant now = Instant.now();
        Pageable pageable = PageRequest.of(0, batchSize);
        Page<FailedWebhookEvent> retryableWebhooks = 
            failedWebhookRepository.findRetryableWebhooks(now, pageable);

        if (retryableWebhooks.isEmpty()) {
            log.debug("No webhooks to retry at this time");
            return;
        }

        for (FailedWebhookEvent event : retryableWebhooks.getContent()) {
            try {
                replayWebhook(event);
            } catch (Exception e) {
                log.error("Error replaying webhook {}: {}", event.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Replay a single webhook event.
     *
     * @param event The failed webhook event to retry
     */
    @Transactional
    public void replayWebhook(FailedWebhookEvent event) {
        log.info("Replaying webhook event: {} (attempt {}/{})", 
                 event.getStripeEventId(), 
                 event.getRetryCount() + 1,
                 event.getMaxRetries());

        // Mark as in progress
        event.setStatus(FailedWebhookEvent.WebhookStatus.IN_PROGRESS);
        failedWebhookRepository.save(event);

        try {
            // Parse the stored JSON payload back to a Stripe Event object
            Event stripeEvent = parseEventFromJson(event.getEventData());
            
            // Process the webhook through the normal handler pipeline
            webhookProcessor.process(stripeEvent);

            // Mark as succeeded
            event.setStatus(FailedWebhookEvent.WebhookStatus.SUCCEEDED);
            event.setSucceededAt(Instant.now());
            failedWebhookRepository.save(event);

            log.info("Successfully replayed webhook event: {}", event.getStripeEventId());

        } catch (Exception e) {
            log.warn("Failed to replay webhook {}: {}", event.getStripeEventId(), e.getMessage());

            event.setRetryCount(event.getRetryCount() + 1);
            event.setFailureReason(e.getMessage());

            if (event.getRetryCount() >= event.getMaxRetries()) {
                // Give up
                event.setStatus(FailedWebhookEvent.WebhookStatus.FAILED_PERMANENT);
                log.error("Webhook {} exceeded max retries, marking as permanently failed", 
                         event.getStripeEventId());
            } else {
                // Schedule next retry with exponential backoff
                event.setStatus(FailedWebhookEvent.WebhookStatus.PENDING);
                event.setNextRetryAt(calculateNextRetry(event.getRetryCount()));
                log.info("Scheduling retry for webhook {} at {}", 
                        event.getStripeEventId(), 
                        event.getNextRetryAt());
            }

            failedWebhookRepository.save(event);
        }
    }

    /**
     * Parse a JSON string to a Stripe Event object.
     * Uses Stripe SDK's Event deserialization.
     *
     * @param json Raw JSON payload from Stripe webhook
     * @return Parsed Event object
     * @throws Exception If JSON parsing fails
     */
    private Event parseEventFromJson(String json) throws Exception {
        try {
            // Use Stripe SDK's Event deserialization
            return Event.GSON.fromJson(json, Event.class);
        } catch (JsonSyntaxException e) {
            log.error("Failed to parse webhook JSON: {}", e.getMessage());
            throw new RuntimeException("Invalid webhook JSON format: " + e.getMessage(), e);
        }
    }

    /**
     * Manually replay a specific failed webhook by ID.
     *
     * @param webhookId The ID of the failed webhook
     * @return Updated webhook event
     */
    @Transactional
    public FailedWebhookEvent manualReplay(String webhookId) {
        FailedWebhookEvent event = failedWebhookRepository.findById(Objects.requireNonNull(webhookId))
                .orElseThrow(() -> new IllegalArgumentException("Webhook not found: " + webhookId));

        log.info("Manual replay requested for webhook: {}", Objects.requireNonNull(webhookId));
        
        if (event.getRetryCount() < event.getMaxRetries()) {
            // Reset for replay
            event.setRetryCount(0);
            event.setStatus(FailedWebhookEvent.WebhookStatus.PENDING);
            event.setNextRetryAt(Instant.now());
            event.setFailureReason("Manual replay requested");
            failedWebhookRepository.save(event);
            log.info("Reset webhook {} for manual replay", webhookId);
        }

        return event;
    }

    /**
     * Get list of failed webhooks that are pending retry.
     *
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Page of pending webhooks
     */
    public Page<FailedWebhookEvent> getPendingWebhooks(int page, int size) {
        return failedWebhookRepository.findByStatus(
            FailedWebhookEvent.WebhookStatus.PENDING,
            PageRequest.of(page, size)
        );
    }

    /**
     * Get list of permanently failed webhooks.
     *
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Page of failed webhooks
     */
    public Page<FailedWebhookEvent> getFailedWebhooks(int page, int size) {
        return failedWebhookRepository.findByStatus(
            FailedWebhookEvent.WebhookStatus.FAILED_PERMANENT,
            PageRequest.of(page, size)
        );
    }

    /**
     * Get statistics about webhook retries.
     *
     * @return Statistics object
     */
    public WebhookRetryStats getRetryStats() {
        return WebhookRetryStats.builder()
                .pendingCount(failedWebhookRepository.countByStatus(FailedWebhookEvent.WebhookStatus.PENDING))
                .successCount(failedWebhookRepository.countByStatus(FailedWebhookEvent.WebhookStatus.SUCCEEDED))
                .failedCount(failedWebhookRepository.countByStatus(FailedWebhookEvent.WebhookStatus.FAILED_PERMANENT))
                .inProgressCount(failedWebhookRepository.countByStatus(FailedWebhookEvent.WebhookStatus.IN_PROGRESS))
                .build();
    }

    /**
     * Calculate next retry time with exponential backoff.
     * 
     * Backoff schedule:
     * - 0 retries: 1 minute
     * - 1 retry: 5 minutes
     * - 2 retries: 30 minutes
     * - 3 retries: 2 hours
     * - 4+ retries: 12 hours
     *
     * @param retryCount Current retry count
     * @return Instant when next retry should occur
     */
    private Instant calculateNextRetry(int retryCount) {
        Duration backoff = switch (retryCount) {
            case 0 -> Duration.ofMinutes(1);
            case 1 -> Duration.ofMinutes(5);
            case 2 -> Duration.ofMinutes(30);
            case 3 -> Duration.ofHours(2);
            default -> Duration.ofHours(12);
        };

        return Instant.now().plus(backoff);
    }

    /**
     * Delete a webhook event from the retry queue.
     *
     * @param webhookId ID of webhook to delete
     */
    @Transactional
    public void deleteWebhook(String webhookId) {
        failedWebhookRepository.deleteById(Objects.requireNonNull(webhookId));
        log.info("Deleted webhook event: {}", Objects.requireNonNull(webhookId));
    }

    /**
     * Get recently failed webhooks (last 24 hours).
     *
     * @param page Page number
     * @param size Page size
     * @return Page of recent failures
     */
    public Page<FailedWebhookEvent> getRecentFailures(int page, int size) {
        Instant oneDayAgo = Instant.now().minus(Duration.ofHours(24));
        Instant now = Instant.now();
        return failedWebhookRepository.findByCreatedAtBetween(
            oneDayAgo,
            now,
            PageRequest.of(page, size)
        );
    }

    /**
     * Statistics about webhook retries
     */
    public static class WebhookRetryStats {
        public final long pendingCount;
        public final long successCount;
        public final long failedCount;
        public final long inProgressCount;

        private WebhookRetryStats(Builder builder) {
            this.pendingCount = builder.pendingCount;
            this.successCount = builder.successCount;
            this.failedCount = builder.failedCount;
            this.inProgressCount = builder.inProgressCount;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private long pendingCount;
            private long successCount;
            private long failedCount;
            private long inProgressCount;

            public Builder pendingCount(long pendingCount) {
                this.pendingCount = pendingCount;
                return this;
            }

            public Builder successCount(long successCount) {
                this.successCount = successCount;
                return this;
            }

            public Builder failedCount(long failedCount) {
                this.failedCount = failedCount;
                return this;
            }

            public Builder inProgressCount(long inProgressCount) {
                this.inProgressCount = inProgressCount;
                return this;
            }

            public WebhookRetryStats build() {
                return new WebhookRetryStats(this);
            }
        }
    }
}
