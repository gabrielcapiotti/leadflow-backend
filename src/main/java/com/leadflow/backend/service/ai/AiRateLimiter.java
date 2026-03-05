package com.leadflow.backend.service.ai;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiRateLimiter {

    private final Map<UUID, Bucket> buckets = new ConcurrentHashMap<>();
    private final int maxCallsPerMinute;

    public AiRateLimiter(
            @Value("${security.rate-limit.ai.per-minute:30}") int maxCallsPerMinute
    ) {
        this.maxCallsPerMinute = Math.max(maxCallsPerMinute, 1);
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.builder()
            .capacity(maxCallsPerMinute)
            .refillGreedy(maxCallsPerMinute, Duration.ofMinutes(1))
            .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public boolean allow(UUID vendorId) {

        if (vendorId == null) {
            return false;
        }

        Bucket bucket = buckets.computeIfAbsent(
                vendorId,
                id -> createBucket()
        );

        return bucket.tryConsume(1);
    }
}