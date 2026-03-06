package com.leadflow.backend.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    /**
     * Buckets de rate limit.
     * Chave composta: scope:key
     */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private final int globalLimitPerMinute;
    private final int aiLimitPerMinute;
    private final int authLimitPerMinute;
    private final int adminLimitPerMinute;
    private final int webhookLimitPerMinute;

    public RateLimitService(
            @Value("${security.rate-limit.global.per-minute:100}")
            int globalLimitPerMinute,

            @Value("${security.rate-limit.ai.per-minute:20}")
            int aiLimitPerMinute,

            @Value("${security.rate-limit.auth.per-minute:20}")
            int authLimitPerMinute,

            @Value("${security.rate-limit.admin.per-minute:60}")
            int adminLimitPerMinute,

            @Value("${security.rate-limit.webhook.per-minute:120}")
            int webhookLimitPerMinute
    ) {

        this.globalLimitPerMinute = Math.max(globalLimitPerMinute, 1);
        this.aiLimitPerMinute = Math.max(aiLimitPerMinute, 1);
        this.authLimitPerMinute = Math.max(authLimitPerMinute, 1);
        this.adminLimitPerMinute = Math.max(adminLimitPerMinute, 1);
        this.webhookLimitPerMinute = Math.max(webhookLimitPerMinute, 1);
    }

    /**
     * Cria um bucket com limite por minuto.
     */
    private Bucket newBucket(int capacityPerMinute) {

        Bandwidth limit = Bandwidth.builder()
                .capacity(capacityPerMinute)
                .refillGreedy(capacityPerMinute, Duration.ofMinutes(1))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Consome token com escopo padrão.
     */
    public boolean tryConsume(String key) {
        return tryConsume(key, "global");
    }

    /**
     * Consome token para um escopo específico.
     */
    public boolean tryConsume(String key, String scope) {

        int limit = resolveLimit(scope);

        String bucketKey = scope + ":" + key;

        Bucket bucket = buckets.computeIfAbsent(
                bucketKey,
                ignored -> newBucket(limit)
        );

        return bucket.tryConsume(1);
    }

    /**
     * Versão avançada com retorno de informações do limite.
     */
    public ConsumptionProbe tryConsumeAndReturnRemaining(String key, String scope) {

        int limit = resolveLimit(scope);

        String bucketKey = scope + ":" + key;

        Bucket bucket = buckets.computeIfAbsent(
                bucketKey,
                ignored -> newBucket(limit)
        );

        return bucket.tryConsumeAndReturnRemaining(1);
    }

    /**
     * Resolve limite baseado no tipo de endpoint.
     */
    private int resolveLimit(String scope) {

        if (scope == null) {
            return globalLimitPerMinute;
        }

        return switch (scope) {

            case "ai" -> aiLimitPerMinute;

            case "auth" -> authLimitPerMinute;

            case "admin" -> adminLimitPerMinute;

            case "webhook" -> webhookLimitPerMinute;

            case "global" -> globalLimitPerMinute;

            default -> globalLimitPerMinute;
        };
    }

}
