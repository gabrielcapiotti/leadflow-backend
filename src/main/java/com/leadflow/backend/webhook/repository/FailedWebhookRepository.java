package com.leadflow.backend.webhook.repository;

import com.leadflow.backend.webhook.entity.FailedWebhookEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing failed webhook events.
 * Provides querying and persistence operations for webhook replay.
 *
 * @author LeadFlow Team
 * @version 1.0
 */
@Repository
public interface FailedWebhookRepository extends JpaRepository<FailedWebhookEvent, String> {

    /**
     * Find all failed webhooks that are ready for retry.
     * Returns events with status PENDING where nextRetryAt is in the past.
     *
     * @param now Current time
     * @param pageable Pagination info
     * @return Page of failed webhook events
     */
    @Query("SELECT f FROM FailedWebhookEvent f " +
           "WHERE f.status = 'PENDING' AND f.nextRetryAt <= :now " +
           "ORDER BY f.nextRetryAt ASC")
    Page<FailedWebhookEvent> findRetryableWebhooks(@Param("now") Instant now, Pageable pageable);

    /**
     * Find all pending webhook events for the given tenant.
     *
     * @param tenantId Tenant ID
     * @return List of pending webhooks
     */
    List<FailedWebhookEvent> findByStatusAndTenantId(
        FailedWebhookEvent.WebhookStatus status,
        String tenantId
    );

    /**
     * Find webhook by Stripe event ID.
     *
     * @param stripeEventId Stripe event ID
     * @return Optional containing the webhook event if found
     */
    Optional<FailedWebhookEvent> findByStripeEventId(String stripeEventId);

    /**
     * Count pending webhooks by event type.
     *
     * @param eventType Event type (e.g., "charge.succeeded")
     * @return Number of pending events
     */
    long countByStatusAndEventType(FailedWebhookEvent.WebhookStatus status, String eventType);

    /**
     * Find permanently failed webhooks for analysis.
     *
     * @param pageable Pagination info
     * @return Page of failed webhooks
     */
    Page<FailedWebhookEvent> findByStatus(FailedWebhookEvent.WebhookStatus status, Pageable pageable);

    /**
     * Find webhooks created within a time range.
     *
     * @param startTime Start of range
     * @param endTime End of range
     * @param pageable Pagination info
     * @return Page of webhooks in range
     */
    @Query("SELECT f FROM FailedWebhookEvent f " +
           "WHERE f.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY f.createdAt DESC")
    Page<FailedWebhookEvent> findByCreatedAtBetween(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime,
        Pageable pageable
    );

    /**
     * Find webhooks by event type and status.
     *
     * @param eventType Event type to search for
     * @param status Webhook status
     * @param pageable Pagination info
     * @return Page of matching webhooks
     */
    Page<FailedWebhookEvent> findByEventTypeAndStatus(
        String eventType,
        FailedWebhookEvent.WebhookStatus status,
        Pageable pageable
    );

    /**
     * Count total failed webhooks.
     *
     * @return Total count
     */
    long countByStatus(FailedWebhookEvent.WebhookStatus status);

    /**
     * Check if webhook event already exists.
     *
     * @param stripeEventId Stripe event ID
     * @return true if exists, false otherwise
     */
    boolean existsByStripeEventId(String stripeEventId);
}
