package com.leadflow.backend.config;

import com.leadflow.backend.service.vendor.SubscriptionService;
import com.leadflow.backend.service.billing.StripeService;
import com.leadflow.backend.security.VendorContext;
import com.leadflow.backend.security.RateLimitService;
import com.leadflow.backend.service.billing.StripeWebhookProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

/**
 * Test configuration that provides mock beans for billing-related components.
 * This is used in @WebMvcTest to avoid loading SubscriptionService and other
 * production beans that require database access.
 * 
 * The mocks are configured with default behavior:
 * - VendorContext returns a fixed test UUID for getCurrentVendorId()
 * - SubscriptionService does nothing on validateActiveSubscription()
 */
@TestConfiguration
public class TestBillingConfig {

    private static final UUID TEST_VENDOR_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Bean
    @Primary
    public VendorContext testVendorContext() {
        VendorContext mock = mock(VendorContext.class);
        when(mock.getCurrentVendorId()).thenReturn(TEST_VENDOR_ID);
        return mock;
    }

    @Bean
    @Primary
    public RateLimitService testRateLimitService() {
        return mock(RateLimitService.class);
    }

    @Bean
    @Primary
    public StripeWebhookProcessor testStripeWebhookProcessor() {
        return mock(StripeWebhookProcessor.class);
    }

    @Bean
    @Primary
    public SubscriptionService testSubscriptionService() {
        SubscriptionService mock = mock(SubscriptionService.class);
        doNothing().when(mock).validateActiveSubscription(TEST_VENDOR_ID);
        return mock;
    }

    @Bean
    @Primary
    public StripeService testStripeService() {
        return mock(StripeService.class);
    }
}
