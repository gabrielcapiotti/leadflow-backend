package com.leadflow.backend.service.auth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;

@Service
public class BruteForceProtectionService {

    private final StringRedisTemplate redisTemplate;
    private final ValueOperations<String, String> valueOps;

    public BruteForceProtectionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate);
        this.valueOps = redisTemplate.opsForValue();
    }

    /* ======================================================
       CHECK BLOCK STATUS
       ====================================================== */

    public boolean isBlocked(String key, int maxAttempts) {

        if (key == null || key.isBlank()) {
            return false;
        }

        try {

            String value = valueOps.get(key);

            if (value == null) {
                return false;
            }

            int attempts = Integer.parseInt(value);

            return attempts >= maxAttempts;

        } catch (Exception e) {

            // Fail-safe: nunca bloquear usuário se Redis falhar
            return false;
        }
    }

    /* ======================================================
       RECORD FAILURE (ATOMIC INCR + TTL)
       ====================================================== */

    public void recordFailure(String key, int windowMinutes) {

        if (key == null || key.isBlank()) {
            return;
        }

        try {

            Long count = valueOps.increment(key);

            if (count != null && count == 1) {

                Duration ttl = Duration.ofMinutes(windowMinutes);
                Duration safeTtl = Objects.requireNonNull(ttl);

                redisTemplate.expire(key, safeTtl);
            }

        } catch (Exception ignored) {

            // Fail-safe: se Redis cair, não quebrar login
        }
    }

    /* ======================================================
       RESET COUNTER
       ====================================================== */

    public void reset(String key) {

        if (key == null || key.isBlank()) {
            return;
        }

        try {

            redisTemplate.delete(key);

        } catch (Exception ignored) {

            // Fail-safe
        }
    }
}