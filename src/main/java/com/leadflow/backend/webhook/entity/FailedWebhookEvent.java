package com.leadflow.backend.webhook.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Entity to store failed webhook events for later replay.
 * Enables recovery from temporary failures without losing data.
 *
 * @author LeadFlow Team
 * @version 1.0
 */
@Entity
@Table(name = "failed_webhook_events", indexes = {
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_next_retry", columnList = "next_retry_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailedWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * Stripe event ID from webhook
     */
    @Column(nullable = false, unique = true)
    private String stripeEventId;

    /**
     * Webhook event type (charge.succeeded, invoice.payment_failed, etc.)
     */
    @Column(nullable = false)
    private String eventType;

    /**
     * Raw webhook body
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String eventData;

    /**
     * Failure reason (connection timeout, database error, etc.)
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String failureReason;

    /**
     * Number of retry attempts
     */
    @Column(nullable = false)
    private int retryCount;

    /**
     * Maximum retry attempts before giving up
     */
    @Column(nullable = false)
    private int maxRetries;

    /**
     * Status: PENDING, IN_PROGRESS, SUCCEEDED, FAILED_PERMANENT
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WebhookStatus status;

    /**
     * When the event should be retried next
     */
    @Column(nullable = false)
    private Instant nextRetryAt;

    /**
     * When the event was originally received
     */
    @Column(nullable = false)
    private Instant originalReceivedAt;

    /**
     * When the event was created in our system
     */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Last update time
     */
    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * Success time (if retried successfully)
     */
    private Instant succeededAt;

    /**
     * Tenant ID (for multi-tenant support)
     */
    private String tenantId;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (nextRetryAt == null) {
            nextRetryAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Webhook processing status
     */
    public enum WebhookStatus {
        PENDING,              // Waiting to be retried
        IN_PROGRESS,          // Currently being processed
        SUCCEEDED,            // Successfully processed
        FAILED_PERMANENT      // Permanently failed (max retries reached)
    }
}
