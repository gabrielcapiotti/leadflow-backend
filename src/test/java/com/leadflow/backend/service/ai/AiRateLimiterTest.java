package com.leadflow.backend.service.ai;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiRateLimiterTest {

    @Test
    void shouldAllowUpToConfiguredLimitPerMinuteForVendor() {

        AiRateLimiter limiter = new AiRateLimiter(3);
        UUID vendorId = UUID.randomUUID();

        assertTrue(limiter.allow(vendorId));
        assertTrue(limiter.allow(vendorId));
        assertTrue(limiter.allow(vendorId));
        assertFalse(limiter.allow(vendorId));
    }

    @Test
    void shouldIsolateBucketsPerVendor() {

        AiRateLimiter limiter = new AiRateLimiter(1);
        UUID vendorA = UUID.randomUUID();
        UUID vendorB = UUID.randomUUID();

        assertTrue(limiter.allow(vendorA));
        assertFalse(limiter.allow(vendorA));

        assertTrue(limiter.allow(vendorB));
        assertFalse(limiter.allow(vendorB));
    }
}
