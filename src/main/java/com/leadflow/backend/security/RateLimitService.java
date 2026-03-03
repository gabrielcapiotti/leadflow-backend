package com.leadflow.backend.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket newBucket() {

        Bandwidth limit = Bandwidth.classic(
                20,
                Refill.greedy(20, Duration.ofMinutes(1))
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public boolean tryConsume(String key) {

        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket());

        return bucket.tryConsume(1);
    }
}
