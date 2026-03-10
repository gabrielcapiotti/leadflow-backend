package com.leadflow.backend.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Centralized Stripe configuration properties.
 * Loads from application.yml with prefix "stripe" and environment variables.
 * 
 * Example: stripe.api.secret-key -> ${STRIPE_SECRET_KEY}
 */
@Configuration
@EnableConfigurationProperties(StripeProperties.class)
@ConfigurationProperties(prefix = "stripe")
@Getter
@Setter
@Validated
@Slf4j
public class StripeProperties {
    
    private Api api = new Api();
    private Webhook webhook = new Webhook();
    private Retry retry = new Retry();
    private Timeout timeout = new Timeout();
    private Events events = new Events();
    
    @PostConstruct
    public void init() {
        log.info("=== Initializing Stripe Configuration ===");
        
        // Validate required fields
        if (api.secretKey == null || api.secretKey.isBlank()) {
            log.error("CRITICAL: stripe.api.secret-key is not configured!");
            throw new IllegalStateException("Stripe secret key is required. Set STRIPE_SECRET_KEY environment variable.");
        }
        
        if (webhook.secret == null || webhook.secret.isBlank()) {
            log.error("CRITICAL: stripe.webhook.secret is not configured!");
            throw new IllegalStateException("Stripe webhook secret is required. Set STRIPE_WEBHOOK_SECRET environment variable.");
        }
        
        // Determine mode (test or live)
        String mode = api.secretKey.startsWith("sk_test_") ? "TEST" : "LIVE";
        log.warn("⚠️  Stripe Mode: {} (Key: {}...)", mode, api.secretKey.substring(0, Math.min(15, api.secretKey.length())));
        
        // Initialize Stripe SDK with secret key
        Stripe.apiKey = api.secretKey;
        log.info("✅ Stripe SDK initialized successfully");
        
        // Log configuration summary
        log.info("Stripe Configuration Summary:");
        log.info("  - API Key: {}... ({})", api.secretKey.substring(0, Math.min(15, api.secretKey.length())), mode);
        log.info("  - Webhook Enabled: {}", webhook.enabled);
        log.info("  - Webhook Timestamp Tolerance: {}s", webhook.timestampToleranceSeconds);
        log.info("  - Retry Max Attempts: {}", retry.maxAttempts);
        log.info("  - Connection Timeout: {}ms", timeout.connectionMs);
        log.info("  - Read Timeout: {}ms", timeout.readMs);
        log.info("  - Events Persistence: {} (Max Age: {} days)", events.enabled, events.maxAgeDays);
    }
    
    /**
     * API Configuration - Stripe API keys and endpoints
     */
    @Getter
    @Setter
    public static class Api {
        @NotBlank(message = "Stripe secret key is required")
        private String secretKey;
        
        @NotBlank(message = "Stripe publishable key is required")
        private String publishableKey;
        
        // Optional fields for future use
        private String restrictedKey;
    }
    
    /**
     * Webhook Configuration - Webhook security and processing settings
     */
    @Getter
    @Setter
    public static class Webhook {
        @NotBlank(message = "Stripe webhook secret is required")
        private String secret;
        
        private String path = "/stripe/webhook";
        
        private boolean enabled = true;
        
        // Tolerance for webhook timestamp validation (prevent replay attacks)
        // Default: 5 minutes
        private long timestampToleranceSeconds = 300;
    }
    
    /**
     * Retry Configuration - Exponential backoff settings for failed operations
     */
    @Getter
    @Setter
    public static class Retry {
        private int maxAttempts = 3;
        
        private long initialDelayMs = 1000;  // 1 second
        
        private double multiplier = 2.0;    // exponential: 1s, 2s, 4s, 8s, etc
        
        /**
         * Calculate delay for specific retry attempt using exponential backoff.
         * @param attempt The attempt number (1-indexed)
         * @return Delay in milliseconds
         */
        public long getDelayForAttempt(int attempt) {
            return (long) (initialDelayMs * Math.pow(multiplier, attempt - 1));
        }
    }
    
    /**
     * Timeout Configuration - HTTP timeout settings for Stripe API calls
     */
    @Getter
    @Setter
    public static class Timeout {
        private long connectionMs = 10000;  // 10 seconds
        
        private long readMs = 30000;        // 30 seconds
    }
    
    /**
     * Events Configuration - Webhook event persistence and lifecycle
     */
    @Getter
    @Setter
    public static class Events {
        private boolean enabled = true;
        
        // Keep webhook events in database for this many days
        private int maxAgeDays = 90;
    }
}
