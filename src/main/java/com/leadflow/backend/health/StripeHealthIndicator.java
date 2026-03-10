package com.leadflow.backend.health;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Health indicator for Stripe API connectivity.
 * 
 * Performs periodic checks to ensure Stripe API is accessible
 * and webhook processing can proceed.
 *
 * @author LeadFlow Team
 * @version 1.0
 */
@Slf4j
@Component
public class StripeHealthIndicator implements HealthIndicator {

    @Autowired(required = false)
    private com.stripe.Stripe stripeStatic;

    private static final long HEALTH_CHECK_TIMEOUT_MS = 5000;

    @Override
    public Health health() {
        try {
            // Test Stripe API connectivity with a simple list request
            // This doesn't charge anything, just validates auth
            performStripeHealthCheck();
            
            return Health.up()
                    .withDetail("service", "Stripe")
                    .withDetail("api_version", getStripeApiVersion())
                    .build();
                    
        } catch (StripeException e) {
            log.warn("Stripe API health check failed: {}", e.getUserMessage());
            return Health.down()
                    .withDetail("service", "Stripe")
                    .withDetail("error", e.getUserMessage())
                    .withDetail("code", e.getCode())
                    .withException(e)
                    .build();
                    
        } catch (Exception e) {
            log.error("Stripe health check error: {}", e.getMessage(), e);
            return Health.down()
                    .withDetail("service", "Stripe")
                    .withDetail("error", "Connection failed: " + e.getMessage())
                    .withException(e)
                    .build();
        }
    }

    /**
     * Perform actual health check against Stripe API.
     * Uses a list request with limit=1 (minimal data transfer).
     *
     * @throws StripeException If API call fails
     */
    private void performStripeHealthCheck() throws StripeException {
        // Simple API call to verify connectivity
        // This retrieves at most 1 customer, which verifies auth and API access
        Customer.list(Collections.singletonMap("limit", 1));
    }

    /**
     * Get current Stripe API version.
     *
     * @return API version string
     */
    private String getStripeApiVersion() {
        try {
            // Return Stripe API version
            return "v1";
        } catch (Exception e) {
            return "unknown";
        }
    }
}
