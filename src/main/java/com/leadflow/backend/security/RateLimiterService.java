package com.leadflow.backend.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimiterService {

    private static final int MAX_REQUESTS_PER_MINUTE = 30;

    private final Map<UUID, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> windowStart = new ConcurrentHashMap<>();

    public boolean allowRequest(UUID vendorId) {

        Instant now = Instant.now();

        windowStart.putIfAbsent(vendorId, now);
        requestCounts.putIfAbsent(vendorId, new AtomicInteger(0));

        Instant start = windowStart.get(vendorId);

        if (start.plusSeconds(60).isBefore(now)) {

            windowStart.put(vendorId, now);
            requestCounts.get(vendorId).set(0);
        }

        int count = requestCounts.get(vendorId).incrementAndGet();

        return count <= MAX_REQUESTS_PER_MINUTE;
    }
}