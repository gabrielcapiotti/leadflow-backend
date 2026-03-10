package com.leadflow.backend.resilience.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j Configuration for Circuit Breaker and Retry patterns.
 * Provides fault tolerance for external API calls (Stripe, Email services).
 *
 * @author LeadFlow Team
 * @version 1.0
 */
@Configuration
@Slf4j
public class Resilience4jConfiguration {

    /**
     * Circuit Breaker for Stripe API calls.
     * Opens after 5 consecutive failures to prevent cascading failures.
     */
    @Bean
    public CircuitBreaker stripeCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f)                    // Open if 50% fail
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before retry
                .permittedNumberOfCallsInHalfOpenState(3)       // Try 3 calls in half-open
                .minimumNumberOfCalls(5)                        // Need 5 calls to evaluate
                .slowCallRateThreshold(50.0f)                   // Open if 50% are slow
                .slowCallDurationThreshold(Duration.ofSeconds(2)) // > 2s is slow
                .recordExceptions(Exception.class)               // Record all exceptions
                .build();

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("stripe", config);
        
        // Log circuit breaker state changes
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> 
                    log.warn("Stripe Circuit Breaker state changed: {} -> {}", 
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState())
                )
                .onError(event ->
                    log.warn("Stripe Circuit Breaker recorded error: {}", 
                        event.getThrowable().getMessage())
                )
                .onSuccess(event ->
                    log.debug("Stripe Circuit Breaker successful call")
                );

        return circuitBreaker;
    }

    /**
     * Circuit Breaker for Email/SMTP service calls.
     * Opens after multiple email delivery failures.
     */
    @Bean
    public CircuitBreaker emailCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(60.0f)                    // Open if 60% fail
                .waitDurationInOpenState(Duration.ofSeconds(60)) // Wait 60s before retry
                .permittedNumberOfCallsInHalfOpenState(2)       // Try 2 calls in half-open
                .minimumNumberOfCalls(3)                        // Need 3 calls to evaluate
                .slowCallRateThreshold(40.0f)                   // Open if 40% are slow
                .slowCallDurationThreshold(Duration.ofSeconds(3)) // > 3s is slow
                .recordExceptions(Exception.class)
                .build();

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("email", config);
        
        circuitBreaker.getEventPublisher()
                .onStateTransition(event ->
                    log.warn("Email Circuit Breaker state changed: {} -> {}",
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState())
                )
                .onError(event ->
                    log.warn("Email Circuit Breaker recorded error: {}",
                        event.getThrowable().getMessage())
                );

        return circuitBreaker;
    }

    /**
     * Circuit Breaker for Database operations.
     * Opens if connection pool becomes saturated or queries timeout.
     */
    @Bean
    public CircuitBreaker databaseCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(70.0f)                    // Open if 70% fail
                .waitDurationInOpenState(Duration.ofSeconds(15)) // Quick recovery
                .permittedNumberOfCallsInHalfOpenState(5)       // Try 5 calls in half-open
                .minimumNumberOfCalls(10)                       // Need 10 calls to evaluate
                .slowCallRateThreshold(30.0f)                   // Open if 30% are slow
                .slowCallDurationThreshold(Duration.ofSeconds(5)) // > 5s is slow
                .recordExceptions(Exception.class)
                .build();

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("database", config);
        
        circuitBreaker.getEventPublisher()
                .onStateTransition(event ->
                    log.warn("Database Circuit Breaker state changed: {} -> {}",
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState())
                );

        return circuitBreaker;
    }

    /**
     * Retry Configuration for transient failures.
     * Stripe API calls will retry up to 3 times with exponential backoff.
     */
    @Bean
    public Retry stripeRetry(RetryRegistry retryRegistry) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)                                   // Retry up to 3 times
                .waitDuration(Duration.ofMillis(500))            // Initial wait: 500ms
                .intervalFunction(io.github.resilience4j.core.IntervalFunction
                        .ofExponentialRandomBackoff(500, 2))    // Exponential backoff: 2x
                .retryOnException(throwable ->
                    shouldRetry(throwable)                       // Retry only on certain exceptions
                )
                .build();

        return retryRegistry.retry("stripe-retry", config);
    }

    /**
     * Retry Configuration for Email operations.
     * Email sends will retry up to 2 times with fixed delay.
     */
    @Bean
    public Retry emailRetry(RetryRegistry retryRegistry) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofSeconds(1))            // Fixed wait: 1s
                .retryOnException(throwable ->
                    shouldRetryEmail(throwable)
                )
                .build();

        return retryRegistry.retry("email-retry", config);
    }

    /**
     * Register event consumer to log circuit breaker events.
     */
    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerEventConsumer() {
        return new RegistryEventConsumer<CircuitBreaker>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
                CircuitBreaker circuitBreaker = entryAddedEvent.getAddedEntry();
                log.info("Circuit Breaker registered: {}", circuitBreaker.getName());
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemovedEvent) {
                CircuitBreaker circuitBreaker = entryRemovedEvent.getRemovedEntry();
                log.info("Circuit Breaker removed: {}", circuitBreaker.getName());
            }

            @Override
            public void onEntryReplacedEvent(io.github.resilience4j.core.registry.EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
                log.info("Circuit Breaker replaced: {}", entryReplacedEvent.getNewEntry().getName());
            }
        };
    }

    /**
     * Register metrics for Circuit Breakers.
     */
    @Bean
    public TaggedCircuitBreakerMetrics circuitBreakerMetrics(CircuitBreakerRegistry circuitBreakerRegistry) {
        return TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(circuitBreakerRegistry);
    }

    /**
     * Determine if an exception should trigger a retry.
     * Retryable: Connection timeouts, temporary service errors (5xx)
     * Non-retryable: Authentication errors (4xx), business logic errors
     */
    private boolean shouldRetry(Throwable throwable) {
        String message = throwable.getMessage() == null ? "" : throwable.getMessage();
        
        // Retry on network/timeout errors
        return throwable instanceof java.net.SocketTimeoutException ||
               throwable instanceof java.net.ConnectException ||
               throwable instanceof java.io.IOException ||
               message.contains("timeout") ||
               message.contains("temporarily unavailable") ||
               message.contains("Service Unavailable") ||
               message.contains("Too Many Requests");
    }

    /**
     * Determine if email exceptions should trigger retry.
     */
    private boolean shouldRetryEmail(Throwable throwable) {
        String message = throwable.getMessage() == null ? "" : throwable.getMessage();
        
        return throwable instanceof java.net.SocketTimeoutException ||
               throwable instanceof java.net.ConnectException ||
               message.contains("SMTP") ||
               message.contains("timeout") ||
               message.contains("try later") ||
               message.contains("Service Unavailable");
    }
}
