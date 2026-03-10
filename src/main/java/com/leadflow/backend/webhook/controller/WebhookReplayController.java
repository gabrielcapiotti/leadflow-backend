package com.leadflow.backend.webhook.controller;

import com.leadflow.backend.multitenancy.TenantContext;
import com.leadflow.backend.webhook.entity.FailedWebhookEvent;
import com.leadflow.backend.webhook.service.WebhookReplayService;
import com.leadflow.backend.webhook.validator.WebhookTenantValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for managing failed webhook events.
 * Provides endpoints for viewing, replaying, and managing webhook retry queue.
 * 
 * All operations are scoped to the current tenant for multi-tenant isolation.
 * Tenant ID is extracted from:
 * - X-Tenant-ID header
 * - JWT claim (tenant_id)
 * - URL path (/api/tenants/{tenantId}/...)
 *
 * @author LeadFlow Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/billing/webhooks")
@Tag(name = "Webhook Management", description = "Manage and replay failed webhook events")
public class WebhookReplayController {

    private final WebhookReplayService webhookReplayService;
    private final WebhookTenantValidator tenantValidator;

    @Autowired
    public WebhookReplayController(
            WebhookReplayService webhookReplayService,
            WebhookTenantValidator tenantValidator) {
        this.webhookReplayService = webhookReplayService;
        this.tenantValidator = tenantValidator;
    }

    /**
     * Get list of pending webhooks waiting to be retried.
     *
     * @param page Page number (0-indexed), default 0
     * @param size Page size, default 20
     * @return Page of pending webhooks
     */
    @GetMapping("/failed")
    @Operation(
        summary = "Get pending webhooks",
        description = "Retrieve webhooks that are pending retry. These are failed events waiting for the next retry attempt."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved pending webhooks"),
        @ApiResponse(responseCode = "400", description = "Invalid page or size parameters")
    })
    public ResponseEntity<Page<FailedWebhookEvent>> getPendingWebhooks(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        log.info("Fetching pending webhooks - page: {}, size: {}", page, size);
        Page<FailedWebhookEvent> webhooks = webhookReplayService.getPendingWebhooks(page, size);
        return ResponseEntity.ok(webhooks);
    }

    /**
     * Get list of permanently failed webhooks.
     *
     * @param page Page number (0-indexed), default 0
     * @param size Page size, default 20
     * @return Page of permanently failed webhooks
     */
    @GetMapping("/failed/permanent")
    @Operation(
        summary = "Get permanently failed webhooks",
        description = "Retrieve webhooks that have exceeded max retry attempts and failed permanently."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved failed webhooks"),
        @ApiResponse(responseCode = "400", description = "Invalid page or size parameters")
    })
    public ResponseEntity<Page<FailedWebhookEvent>> getFailedWebhooks(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        log.info("Fetching permanently failed webhooks - page: {}, size: {}", page, size);
        Page<FailedWebhookEvent> webhooks = webhookReplayService.getFailedWebhooks(page, size);
        return ResponseEntity.ok(webhooks);
    }

    /**
     * Get recently failed webhooks (last 24 hours).
     *
     * @param page Page number (0-indexed), default 0
     * @param size Page size, default 20
     * @return Page of recent failures
     */
    @GetMapping("/failed/recent")
    @Operation(
        summary = "Get recently failed webhooks",
        description = "Retrieve webhooks that failed in the last 24 hours."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved recent failures")
    })
    public ResponseEntity<Page<FailedWebhookEvent>> getRecentFailures(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        log.info("Fetching recent webhook failures - page: {}, size: {}", page, size);
        Page<FailedWebhookEvent> webhooks = webhookReplayService.getRecentFailures(page, size);
        return ResponseEntity.ok(webhooks);
    }

    /**
     * Manually replay a failed webhook event.
     * Resets the webhook to pending status and schedules immediate retry.
     *
     * @param webhookId ID of the webhook to replay
     * @return Updated webhook event
     */
    @PostMapping("/{webhookId}/replay")
    @Operation(
        summary = "Manually replay a webhook",
        description = "Manually replay a failed webhook event. Resets the retry counter and schedules immediate retry. " +
                      "Operation is scoped to current tenant."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Webhook scheduled for replay",
            content = @Content(schema = @Schema(implementation = FailedWebhookEvent.class))
        ),
        @ApiResponse(responseCode = "403", description = "Webhook does not belong to tenant"),
        @ApiResponse(responseCode = "404", description = "Webhook not found"),
        @ApiResponse(responseCode = "500", description = "Error replaying webhook")
    })
    public ResponseEntity<FailedWebhookEvent> replayWebhook(
            @Parameter(description = "ID of the webhook to replay", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String webhookId) {

        log.info("Manual replay requested for webhook: {}", webhookId);
        try {
            String tenantId = TenantContext.getCurrentTenant();
            
            // Validate webhook belongs to current tenant
            tenantValidator.validateFailedWebhookTenant(webhookId, tenantId);
            
            FailedWebhookEvent webhookEvent = webhookReplayService.manualReplay(webhookId);
            log.info("Webhook {} scheduled for replay by tenant {}", webhookId, tenantId);
            return ResponseEntity.ok(webhookEvent);
            
        } catch (WebhookTenantValidator.WebhookTenantViolationException e) {
            log.warn("Tenant isolation violation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            log.error("Webhook not found: {}", webhookId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error replaying webhook {}: {}", webhookId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get webhook retry statistics.
     *
     * @return Statistics object with counts
     */
    @GetMapping("/stats")
    @Operation(
        summary = "Get webhook retry statistics",
        description = "Retrieve statistics about webhook retry queue: pending, succeeded, failed, and in-progress counts."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved statistics",
            content = @Content(schema = @Schema(implementation = WebhookRetryStatsResponse.class))
        )
    })
    public ResponseEntity<WebhookRetryStatsResponse> getRetryStats() {
        log.info("Fetching webhook retry statistics");
        var stats = webhookReplayService.getRetryStats();
        return ResponseEntity.ok(WebhookRetryStatsResponse.fromServiceStats(stats));
    }

    /**
     * Delete a webhook event from the retry queue.
     * Use this to remove permanently failed webhooks that cannot be recovered.
     *
     * @param webhookId ID of webhook to delete
     * @return No content
     */
    @DeleteMapping("/{webhookId}")
    @Operation(
        summary = "Delete a webhook event",
        description = "Remove a webhook event from the retry queue. Use only for webhooks that cannot be recovered. " +
                      "Operation is scoped to current tenant."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Webhook successfully deleted"),
        @ApiResponse(responseCode = "403", description = "Webhook does not belong to tenant"),
        @ApiResponse(responseCode = "404", description = "Webhook not found")
    })
    public ResponseEntity<Void> deleteWebhook(
            @Parameter(description = "ID of the webhook to delete", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String webhookId) {

        log.info("Delete requested for webhook: {}", webhookId);
        try {
            String tenantId = TenantContext.getCurrentTenant();
            
            // Validate webhook belongs to current tenant
            tenantValidator.validateFailedWebhookTenant(webhookId, tenantId);
            
            webhookReplayService.deleteWebhook(webhookId);
            log.info("Webhook {} deleted successfully by tenant {}", webhookId, tenantId);
            return ResponseEntity.noContent().build();
            
        } catch (WebhookTenantValidator.WebhookTenantViolationException e) {
            log.warn("Tenant isolation violation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Error deleting webhook {}: {}", webhookId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Response DTO for webhook retry statistics
     */
    public static class WebhookRetryStatsResponse {
        public long pendingCount;
        public long successCount;
        public long failedCount;
        public long inProgressCount;

        public WebhookRetryStatsResponse(long pendingCount, long successCount, long failedCount, long inProgressCount) {
            this.pendingCount = pendingCount;
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.inProgressCount = inProgressCount;
        }

        public static WebhookRetryStatsResponse fromServiceStats(WebhookReplayService.WebhookRetryStats stats) {
            return new WebhookRetryStatsResponse(
                stats.pendingCount,
                stats.successCount,
                stats.failedCount,
                stats.inProgressCount
            );
        }
    }
}
