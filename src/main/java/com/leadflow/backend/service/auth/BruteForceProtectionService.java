package com.leadflow.backend.service.auth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class BruteForceProtectionService {

    private final StringRedisTemplate redisTemplate;
    private final ValueOperations<String, String> valueOps;

    public BruteForceProtectionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.valueOps = redisTemplate.opsForValue();
    }

    /* ======================================================
       CHECK BLOCK STATUS
       ====================================================== */

    public boolean isBlocked(String key, int maxAttempts) {

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

        try {
            Long count = valueOps.increment(key);

            if (count != null && count == 1) {
                redisTemplate.expire(
                        key,
                        Duration.ofMinutes(windowMinutes)
                );
            }

        } catch (Exception ignored) {
            // Fail-safe: se Redis cair, não quebrar login
        }
    }

    /* ======================================================
       RESET COUNTER
       ====================================================== */

    public void reset(String key) {

        try {
            redisTemplate.delete(key);
        } catch (Exception ignored) {
            // Fail-safe
        }
    }
}