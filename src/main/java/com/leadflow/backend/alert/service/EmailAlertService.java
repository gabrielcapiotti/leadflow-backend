package com.leadflow.backend.alert.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import lombok.extern.slf4j.Slf4j;

import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Email alert service for critical system events
 * 
 * Sends alerts about:
 * - Critical errors
 * - High webhook error rates
 * - Payment processing failures
 * - Signature validation issues
 * - System health degradation
 */
@Slf4j
@Service
@ConditionalOnBean(JavaMailSender.class)
public class EmailAlertService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String alertEmailAddress;
    private final String applicationName;
    private final String environment;

    public EmailAlertService(
        JavaMailSender mailSender,
        TemplateEngine templateEngine,
        @Value("${alert.email.to:}") String alertEmailAddress,
        @Value("${spring.application.name:leadflow-backend}") String applicationName,
        @Value("${spring.profiles.active:unknown}") String environment) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.alertEmailAddress = alertEmailAddress;
        this.applicationName = applicationName;
        this.environment = environment;
    }

    /**
     * Send alert for high webhook error rate
     */
    public void alertHighWebhookErrorRate(double errorRate, int totalErrors, String details) {
        try {
            String subject = String.format(
                "[ALERT] High Webhook Error Rate: %.2f%% - %s",
                errorRate * 100, applicationName
            );

            Map<String, Object> variables = new LinkedHashMap<>();
            variables.put("alertTitle", "High Webhook Error Rate");
            variables.put("severity", "HIGH");
            variables.put("errorRate", String.format("%.2f%%", errorRate * 100));
            variables.put("totalErrors", totalErrors);
            variables.put("details", details);
            variables.put("timestamp", getCurrentTimestamp());
            variables.put("environment", environment.toUpperCase());

            sendEmailAlert(subject, "alert/error-rate", variables);
            log.warn("Email alert sent: High webhook error rate");
        } catch (Exception e) {
            log.error("Failed to send email alert for high webhook error rate", e);
        }
    }

    /**
     * Send alert for signature validation failures
     */
    public void alertSignatureValidationFailures(int failureCount, List<String> failedSignatures) {
        try {
            String subject = String.format(
                "[ALERT] Signature Validation Failures: %d occurrences - %s",
                failureCount, applicationName
            );

            Map<String, Object> variables = new LinkedHashMap<>();
            variables.put("alertTitle", "Signature Validation Failures");
            variables.put("severity", "CRITICAL");
            variables.put("failureCount", failureCount);
            variables.put("failedSignatures", failedSignatures);
            variables.put("timestamp", getCurrentTimestamp());
            variables.put("environment", environment.toUpperCase());
            variables.put("action", "Verify Stripe webhook secret and check network issues");

            sendEmailAlert(subject, "alert/signature-failure", variables);
            log.error("Email alert sent: Signature validation failures");
        } catch (Exception e) {
            log.error("Failed to send email alert for signature validation failures", e);
        }
    }

    /**
     * Send alert for no webhooks processed
     */
    public void alertNoWebhooksProcessed(long minutesWithoutWebhooks) {
        try {
            String subject = String.format(
                "[CRITICAL] No Webhooks Processed for %d minutes - %s",
                minutesWithoutWebhooks, applicationName
            );

            Map<String, Object> variables = new LinkedHashMap<>();
            variables.put("alertTitle", "Webhook Processing Stopped");
            variables.put("severity", "CRITICAL");
            variables.put("minutesWithoutWebhooks", minutesWithoutWebhooks);
            variables.put("timestamp", getCurrentTimestamp());
            variables.put("environment", environment.toUpperCase());
            variables.put("action", "Check Stripe webhook configuration, network connectivity, and application logs");
            variables.put("escalation", "Page on-call engineer immediately");

            sendEmailAlert(subject, "alert/no-webhooks", variables);
            log.error("Email alert sent: No webhooks processed");
        } catch (Exception e) {
            log.error("Failed to send email alert for no webhooks processed", e);
        }
    }

    /**
     * Send alert for email delivery failures
     */
    public void alertEmailDeliveryFailures(int failureCount, List<String> failedEmails) {
        try {
            String subject = String.format(
                "[WARNING] Email Delivery Failures: %d emails failed - %s",
                failureCount, applicationName
            );

            Map<String, Object> variables = new LinkedHashMap<>();
            variables.put("alertTitle", "Email Delivery Failures");
            variables.put("severity", "WARNING");
            variables.put("failureCount", failureCount);
            variables.put("failedEmails", failedEmails);
            variables.put("timestamp", getCurrentTimestamp());
            variables.put("environment", environment.toUpperCase());
            variables.put("action", "Check SMTP configuration and email server status");

            sendEmailAlert(subject, "alert/email-failure", variables);
            log.warn("Email alert sent: Email delivery failures");
        } catch (Exception e) {
            log.error("Failed to send email alert for email delivery failures", e);
        }
    }

    /**
     * Send alert for payment processing failures
     */
    public void alertPaymentProcessingFailures(int failureCount, List<Map<String, String>> failures) {
        try {
            String subject = String.format(
                "[WARNING] Payment Processing Failures: %d payments failed - %s",
                failureCount, applicationName
            );

            Map<String, Object> variables = new LinkedHashMap<>();
            variables.put("alertTitle", "Payment Processing Failures");
            variables.put("severity", "WARNING");
            variables.put("failureCount", failureCount);
            variables.put("failures", failures);
            variables.put("timestamp", getCurrentTimestamp());
            variables.put("environment", environment.toUpperCase());
            variables.put("action", "Review payment logs and contact affected customers");

            sendEmailAlert(subject, "alert/payment-failure", variables);
            log.warn("Email alert sent: Payment processing failures");
        } catch (Exception e) {
            log.error("Failed to send email alert for payment processing failures", e);
        }
    }

    /**
     * Send alert for high latency
     */
    public void alertHighLatency(double p95LatencyMs, double p99LatencyMs) {
        try {
            String subject = String.format(
                "[WARNING] High Webhook Latency: P95=%.0fms - %s",
                p95LatencyMs, applicationName
            );

            Map<String, Object> variables = new LinkedHashMap<>();
            variables.put("alertTitle", "High Webhook Processing Latency");
            variables.put("severity", "WARNING");
            variables.put("p95Latency", String.format("%.0f ms", p95LatencyMs));
            variables.put("p99Latency", String.format("%.0f ms", p99LatencyMs));
            variables.put("timestamp", getCurrentTimestamp());
            variables.put("environment", environment.toUpperCase());
            variables.put("action", "Check database query performance, JVM GC, and network latency");

            sendEmailAlert(subject, "alert/latency", variables);
            log.warn("Email alert sent: High latency detected");
        } catch (Exception e) {
            log.error("Failed to send email alert for high latency", e);
        }
    }

    /**
     * Send alert for system resource issues
     */
    public void alertSystemResources(String resourceType, double usagePercentage) {
        try {
            String subject = String.format(
                "[WARNING] High %s Usage: %.0f%% - %s",
                resourceType, usagePercentage, applicationName
            );

            Map<String, Object> variables = new LinkedHashMap<>();
            variables.put("alertTitle", "System Resource Alert");
            variables.put("severity", usagePercentage > 95 ? "CRITICAL" : "WARNING");
            variables.put("resourceType", resourceType);
            variables.put("usagePercentage", String.format("%.0f%%", usagePercentage));
            variables.put("timestamp", getCurrentTimestamp());
            variables.put("environment", environment.toUpperCase());
            variables.put("action", usagePercentage > 95 ? 
                "Page on-call engineer immediately. Consider scaling up." :
                "Monitor situation. Scale up if continues to rise.");

            sendEmailAlert(subject, "alert/resources", variables);
            log.warn("Email alert sent: High {} usage ({}%)", resourceType, usagePercentage);
        } catch (Exception e) {
            log.error("Failed to send email alert for system resources", e);
        }
    }

    /**
     * Send email alert using template
     */
    private void sendEmailAlert(
        String subject,
        String templateName,
        Map<String, Object> variables) throws Exception {
        
        if (alertEmailAddress == null || alertEmailAddress.isEmpty()) {
            log.debug("Alert email address not configured, skipping alert");
            return;
        }

        // Add common variables
        variables.put("applicationName", applicationName);
        variables.put("environment", environment.toUpperCase());
        variables.put("supportEmail", "support@leadflow.com");

        // Render template
        Context context = new Context();
        context.setVariables(variables);
        String htmlContent = templateEngine.process(templateName, context);

        // Send email
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(alertEmailAddress);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        helper.setFrom("alerts@leadflow.com");

        mailSender.send(message);
        log.info("Email alert sent to {}", alertEmailAddress);
    }

    /**
     * Get current timestamp formatted for display
     */
    private String getCurrentTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss zzz")
            .withZone(ZoneId.of("UTC"));
        return formatter.format(Instant.now());
    }

    /**
     * Check if email alerting is configured
     */
    public boolean isConfigured() {
        return alertEmailAddress != null && !alertEmailAddress.isEmpty();
    }
}
