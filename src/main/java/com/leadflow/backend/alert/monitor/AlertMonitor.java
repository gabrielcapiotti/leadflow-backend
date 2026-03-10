package com.leadflow.backend.alert.monitor;

import com.leadflow.backend.alert.service.SlackAlertService;
import com.leadflow.backend.alert.service.EmailAlertService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Monitor for webhook and billing system health
 * 
 * Periodically checks metrics and triggers alerts when:
 * - Error rate exceeds threshold
 * - Latency exceeds threshold
 * - No webhooks processed
 * - Database connection pool saturation
 * - Memory usage too high
 * 
 * Runs every 30 seconds by default
 */
@Slf4j
@Service
public class AlertMonitor {

    private final SlackAlertService slackAlertService;
    private final Optional<EmailAlertService> emailAlertService;
    private final MeterRegistry meterRegistry;

    // Thresholds
    @Value("${alert.webhook.error-rate-threshold:0.05}")
    private double errorRateThreshold;  // 5% default

    @Value("${alert.webhook.latency-threshold-ms:500}")
    private double latencyThresholdMs;  // 500ms default

    @Value("${alert.webhook.no-event-threshold-mins:10}")
    private long noEventThresholdMins;  // 10 minutes default

    @Value("${alert.database.pool-saturation-threshold:0.85}")
    private double dbPoolSaturationThreshold;  // 85% default

    @Value("${alert.memory.usage-threshold:0.90}")
    private double memoryUsageThreshold;  // 90% default

    // State tracking (to avoid duplicate alerts)
    private final AtomicLong lastHighErrorRateAlert = new AtomicLong(0);
    private final AtomicLong lastHighLatencyAlert = new AtomicLong(0);
    private final AtomicLong lastNoWebhooksAlert = new AtomicLong(0);
    private final AtomicLong lastWebhookTimestamp = new AtomicLong(System.currentTimeMillis());

    public AlertMonitor(
        SlackAlertService slackAlertService,
        Optional<EmailAlertService> emailAlertService,
        MeterRegistry meterRegistry) {
        this.slackAlertService = slackAlertService;
        this.emailAlertService = emailAlertService;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Monitor webhook processing metrics every 30 seconds
     */
    @Scheduled(fixedDelayString = "${alert.monitor.interval-ms:30000}")
    public void monitorWebhookMetrics() {
        try {
            checkWebhookErrorRate();
            checkWebhookLatency();
            checkNoWebhooksProcessed();
            checkMemoryUsage();
        } catch (Exception e) {
            log.error("Error during webhook metrics monitoring", e);
        }
    }

    /**
     * Check webhook error rate and alert if high
     */
    private void checkWebhookErrorRate() {
        try {
            double successCount = getSuccessCount();
            double failureCount = getFailureCount();
            double total = successCount + failureCount;
            
            if (total == 0) {
                return;  // Not enough data
            }

            double errorRate = failureCount / total;

            if (errorRate > errorRateThreshold) {
                long now = System.currentTimeMillis();
                // Alert every 5 minutes maximum
                if (now - lastHighErrorRateAlert.get() > 300_000) {
                    int totalErrors = (int) failureCount;
                    
                    if (slackAlertService.isConfigured()) {
                        slackAlertService.alertHighWebhookErrorRate(errorRate, totalErrors);
                    }
                    
                    emailAlertService.ifPresent(service -> {
                        if (service.isConfigured()) {
                            service.alertHighWebhookErrorRate(
                                errorRate,
                                totalErrors,
                                "Check webhook processing logs for error details"
                            );
                        }
                    });
                    
                    lastHighErrorRateAlert.set(now);
                }
            }
        } catch (Exception e) {
            log.error("Error checking webhook error rate", e);
        }
    }

    /**
     * Check webhook processing latency and alert if high
     */
    private void checkWebhookLatency() {
        try {
            // Get latency from metrics - use average/max
            var timer = meterRegistry.find("webhook_processing_duration_seconds").timer();
            
            if (timer != null) {
                double maxLatency = timer.max(java.util.concurrent.TimeUnit.MILLISECONDS);
                double meanLatency = timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS);
                
                // Use mean as P95 estimate and max as P99
                if (maxLatency > latencyThresholdMs) {
                    long now = System.currentTimeMillis();
                    if (now - lastHighLatencyAlert.get() > 300_000) {
                        
                        if (slackAlertService.isConfigured()) {
                            slackAlertService.alertHighWebhookLatency(meanLatency, maxLatency);
                        }
                        
                        emailAlertService.ifPresent(service -> {
                            if (service.isConfigured()) {
                                service.alertHighLatency(meanLatency, maxLatency);
                            }
                        });
                        
                        lastHighLatencyAlert.set(now);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error checking webhook latency", e);
        }
    }

    /**
     * Check if webhooks are being processed and alert if not
     */
    private void checkNoWebhooksProcessed() {
        try {
            long lastWebhookTime = lastWebhookTimestamp.get();
            long currentTime = System.currentTimeMillis();
            long elapsedMinutes = (currentTime - lastWebhookTime) / (60 * 1000);

            if (elapsedMinutes > noEventThresholdMins) {
                long now = System.currentTimeMillis();
                // Alert every 15 minutes maximum
                if (now - lastNoWebhooksAlert.get() > 900_000) {
                    
                    if (slackAlertService.isConfigured()) {
                        slackAlertService.alertNoWebhooksProcessed(elapsedMinutes);
                    }
                    
                    emailAlertService.ifPresent(service -> {
                        if (service.isConfigured()) {
                            service.alertNoWebhooksProcessed(elapsedMinutes);
                        }
                    });
                    
                    lastNoWebhooksAlert.set(now);
                }
            }
        } catch (Exception e) {
            log.error("Error checking for webhook processing", e);
        }
    }

    /**
     * Check memory usage and alert if high
     */
    private void checkMemoryUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            double usage = (double) usedMemory / maxMemory;

            if (usage > memoryUsageThreshold) {
                if (slackAlertService.isConfigured()) {
                    slackAlertService.alertHighMemoryUsage(usedMemory, maxMemory);
                }
                
                emailAlertService.ifPresent(service -> {
                    if (service.isConfigured()) {
                        service.alertSystemResources("Memory", usage * 100);
                    }
                });
            }
        } catch (Exception e) {
            log.error("Error checking memory usage", e);
        }
    }

    /**
     * Update webhook processing timestamp (call when webhook processed)
     */
    public void recordWebhookProcessed() {
        lastWebhookTimestamp.set(System.currentTimeMillis());
    }

    /**
     * Get failure count from metrics
     */
    private double getFailureCount() {
        var counter = meterRegistry.find("webhook_processing_failure_total").counter();
        if (counter != null) {
            return counter.count();
        }
        return 0.0;
    }

    /**
     * Get success count from metrics
     */
    private double getSuccessCount() {
        var counter = meterRegistry.find("webhook_processing_success_total").counter();
        if (counter != null) {
            return counter.count();
        }
        return 0.0;
    }
}

