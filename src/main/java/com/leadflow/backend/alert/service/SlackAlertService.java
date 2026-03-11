package com.leadflow.backend.alert.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.*;
import java.util.Objects;

/**
 * Slack notification service for webhook and billing alerts
 * 
 * Sends alerts to Slack about:
 * - High webhook error rates
 * - Payment failures
 * - Signature validation failures
 * - Email delivery issues
 * - Performance degradation
 */
@Slf4j
@Service
public class SlackAlertService {

    private final String slackWebhookUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String environment;
    private final String applicationName;

    public SlackAlertService(
        @Value("${slack.webhook.url:}") String slackWebhookUrl,
        @Value("${spring.profiles.active:unknown}") String environment,
        @Value("${spring.application.name:leadflow-backend}") String applicationName,
        RestTemplate restTemplate,
        ObjectMapper objectMapper) {
        this.slackWebhookUrl = slackWebhookUrl;
        this.environment = environment;
        this.applicationName = applicationName;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Send alert for high webhook error rate
     */
    public void alertHighWebhookErrorRate(double errorRate, int totalErrors) {
        try {
            String message = String.format(
                "🚨 **High Webhook Error Rate**: %.2f%% (%d errors)",
                errorRate * 100, totalErrors
            );
            
            sendAlert(
                message,
                "error",
                Map.of(
                    "Error Rate", String.format("%.2f%%", errorRate * 100),
                    "Total Errors", String.valueOf(totalErrors),
                    "Time", Instant.now().toString()
                )
            );
            
            log.warn("Alert sent to Slack: High webhook error rate");
        } catch (Exception e) {
            log.error("Failed to send Slack alert for high webhook error rate", e);
        }
    }

    /**
     * Send alert for high webhook latency
     */
    public void alertHighWebhookLatency(double p95LatencyMs, double p99LatencyMs) {
        try {
            String message = String.format(
                "⏱️ **High Webhook Latency**: P95=%.0fms, P99=%.0fms",
                p95LatencyMs, p99LatencyMs
            );
            
            sendAlert(
                message,
                "warning",
                Map.of(
                    "P95 Latency", String.format("%.0f ms", p95LatencyMs),
                    "P99 Latency", String.format("%.0f ms", p99LatencyMs),
                    "Time", Instant.now().toString()
                )
            );
            
            log.warn("Alert sent to Slack: High webhook latency");
        } catch (Exception e) {
            log.error("Failed to send Slack alert for high webhook latency", e);
        }
    }

    /**
     * Send alert for signature validation failures
     */
    public void alertSignatureValidationFailures(int failureCount) {
        try {
            String message = String.format(
                "🔐 **Signature Validation Failures**: %d failures detected",
                failureCount
            );
            
            sendAlert(
                message,
                "error",
                Map.of(
                    "Failure Count", String.valueOf(failureCount),
                    "Time", Instant.now().toString(),
                    "Action", "Check webhook secret configuration"
                )
            );
            
            log.warn("Alert sent to Slack: Signature validation failures");
        } catch (Exception e) {
            log.error("Failed to send Slack alert for signature validation failures", e);
        }
    }

    /**
     * Send alert for no webhooks processed
     */
    public void alertNoWebhooksProcessed(long minutesWithoutWebhooks) {
        try {
            String message = String.format(
                "⚠️ **No Webhooks Processed**: No events in last %d minutes",
                minutesWithoutWebhooks
            );
            
            sendAlert(
                message,
                "critical",
                Map.of(
                    "Minutes Without Events", String.valueOf(minutesWithoutWebhooks),
                    "Time", Instant.now().toString(),
                    "Action", "Check Stripe webhook configuration and network connectivity"
                )
            );
            
            log.error("Alert sent to Slack: No webhooks processed");
        } catch (Exception e) {
            log.error("Failed to send Slack alert for no webhooks", e);
        }
    }

    /**
     * Send alert for email delivery failures
     */
    public void alertEmailDeliveryFailure(String emailAddress, String reason) {
        try {
            String message = String.format(
                "📧 **Email Delivery Failed**: %s (Reason: %s)",
                emailAddress, reason
            );
            
            sendAlert(
                message,
                "warning",
                Map.of(
                    "Email", emailAddress,
                    "Reason", reason,
                    "Time", Instant.now().toString()
                )
            );
            
            log.warn("Alert sent to Slack: Email delivery failure for {}", emailAddress);
        } catch (Exception e) {
            log.error("Failed to send Slack alert for email delivery failure", e);
        }
    }

    /**
     * Send alert for payment processing failure
     */
    public void alertPaymentProcessingFailure(String customerId, String reason) {
        try {
            String message = String.format(
                "💳 **Payment Processing Failed**: Customer %s",
                customerId
            );
            
            sendAlert(
                message,
                "warning",
                Map.of(
                    "Customer ID", customerId,
                    "Reason", reason,
                    "Time", Instant.now().toString(),
                    "Action", "Review customer account and retry payment"
                )
            );
            
            log.warn("Alert sent to Slack: Payment failure for customer {}", customerId);
        } catch (Exception e) {
            log.error("Failed to send Slack alert for payment failure", e);
        }
    }

    /**
     * Send alert for database connection pool saturation
     */
    public void alertDatabaseConnectionPoolSaturation(int activeConnections, int maxConnections) {
        try {
            int percentage = (activeConnections * 100) / maxConnections;
            String message = String.format(
                "🗄️ **Database Connection Pool Saturation**: %d%% (%d/%d connections)",
                percentage, activeConnections, maxConnections
            );
            
            sendAlert(
                message,
                percentage > 90 ? "critical" : "warning",
                Map.of(
                    "Active Connections", String.valueOf(activeConnections),
                    "Max Connections", String.valueOf(maxConnections),
                    "Usage", String.valueOf(percentage) + "%",
                    "Time", Instant.now().toString()
                )
            );
            
            log.warn("Alert sent to Slack: Database connection pool saturation at {}%", percentage);
        } catch (Exception e) {
            log.error("Failed to send Slack alert for database connection pool", e);
        }
    }

    /**
     * Send alert for memory usage
     */
    public void alertHighMemoryUsage(long usedMemory, long maxMemory) {
        try {
            int percentage = (int) ((usedMemory * 100) / maxMemory);
            String message = String.format(
                "💾 **High Memory Usage**: %d%% (%s MB / %s MB)",
                percentage,
                usedMemory / (1024 * 1024),
                maxMemory / (1024 * 1024)
            );
            
            sendAlert(
                message,
                percentage > 90 ? "critical" : "warning",
                Map.of(
                    "Used Memory", String.format("%d MB", usedMemory / (1024 * 1024)),
                    "Max Memory", String.format("%d MB", maxMemory / (1024 * 1024)),
                    "Usage", String.valueOf(percentage) + "%",
                    "Time", Instant.now().toString()
                )
            );
            
            log.warn("Alert sent to Slack: Memory usage at {}%", percentage);
        } catch (Exception e) {
            log.error("Failed to send Slack alert for memory usage", e);
        }
    }

    /**
     * Send general alert to Slack
     */
    private void sendAlert(String message, String severity, Map<String, String> fields) throws Exception {
        if (slackWebhookUrl == null || slackWebhookUrl.isEmpty()) {
            log.debug("Slack webhook URL not configured, skipping alert");
            return;
        }

        // Build Slack message in Block Kit format
        Map<String, Object> slackMessage = buildSlackMessage(message, severity, fields);
        
        String payload = objectMapper.writeValueAsString(slackMessage);
        
        try {
            restTemplate.postForObject(Objects.requireNonNull(slackWebhookUrl), payload, String.class);
            log.debug("Slack alert sent successfully");
        } catch (RestClientException e) {
            log.error("Failed to send Slack alert", e);
        }
    }

    private Map<String, Object> buildSlackMessage(
        String message,
        String severity,
        Map<String, String> fields) {
        
        String color = switch (severity) {
            case "critical" -> "#FF0000";  // Red
            case "error" -> "#FF6B6B";     // Dark red
            case "warning" -> "#FFA500";   // Orange
            default -> "#0099FF";          // Blue
        };

        List<Map<String, Object>> fieldsList = new ArrayList<>();
        fields.forEach((key, value) -> {
            fieldsList.add(Map.of(
                "title", key,
                "value", value,
                "short", false
            ));
        });

        return Map.of(
            "attachments", List.of(Map.of(
                "color", color,
                "title", applicationName + " - " + environment.toUpperCase(),
                "text", message,
                "fields", fieldsList,
                "footer", "LeadFlow Alert System",
                "ts", System.currentTimeMillis() / 1000
            ))
        );
    }

    /**
     * Check if Slack integration is configured
     */
    public boolean isConfigured() {
        return slackWebhookUrl != null && !slackWebhookUrl.isEmpty();
    }
}
