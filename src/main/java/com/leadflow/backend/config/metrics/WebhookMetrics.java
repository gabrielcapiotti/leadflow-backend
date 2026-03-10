package com.leadflow.backend.config.metrics;

import io.micrometer.core.instrument.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

/**
 * Métricas para processamento de webhooks do Stripe.
 * 
 * Exporta as seguintes métricas para Prometheus:
 * - webhook.processing.duration: Tempo de processamento de webhooks
 * - webhook.processing.count: Total de webhooks processados
 * - webhook.processing.failures: Total de falhas de processamento
 * - webhook.event.type: Distribuição de eventos por tipo
 */
@Component
public class WebhookMetrics {

    private final MeterRegistry meterRegistry;
    private final AtomicInteger pendingWebhooks = new AtomicInteger(0);

    public WebhookMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        // Register gauge for pending webhooks
        Gauge.builder("webhook.pending", pendingWebhooks, AtomicInteger::get)
                .description("Número de webhooks pendentes de processamento")
                .register(meterRegistry);
    }

    /**
     * Cria um Timer para medir o tempo de processamento de webhooks
     * 
     * @param eventType Tipo do evento (checkout.session.completed, invoice.payment_succeeded, etc.)
     * @return Timer para iniciar/parar medição
     */
    public Timer createProcessingTimer(String eventType) {
        return Timer.builder("webhook.processing.duration")
                .description("Tempo de processamento do webhook")
                .tag("event_type", eventType)
                .publishPercentiles(0.5, 0.95, 0.99)
                .minimumExpectedValue(java.time.Duration.ofMillis(10))
                .maximumExpectedValue(java.time.Duration.ofSeconds(30))
                .register(meterRegistry);
    }

    /**
     * Incrementa o contador de webhooks processados com sucesso
     * 
     * @param eventType Tipo do evento
     */
    public void incrementSuccessCounter(String eventType) {
        Counter.builder("webhook.processing.success")
                .description("Total de webhooks processados com sucesso")
                .tag("event_type", eventType)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Incrementa o contador de falhas no processamento de webhooks
     * 
     * @param eventType Tipo do evento
     * @param errorType Tipo do erro (validation_error, processing_error, etc.)
     */
    public void incrementFailureCounter(String eventType, String errorType) {
        Counter.builder("webhook.processing.failure")
                .description("Total de falhas no processamento de webhooks")
                .tag("event_type", eventType)
                .tag("error_type", errorType)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Registra o tipo de evento recebido
     * 
     * @param eventType Tipo do evento Stripe
     */
    public void recordEventType(String eventType) {
        Counter.builder("webhook.event.received")
                .description("Total de eventos webhook recebidos por tipo")
                .tag("event_type", eventType)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Registra um evento de validação de assinatura
     * 
     * @param valid true se a assinatura é válida, false caso contrário
     */
    public void recordSignatureValidation(boolean valid) {
        Counter.builder("webhook.signature.validation")
                .description("Total de validações de assinatura webhook")
                .tag("valid", String.valueOf(valid))
                .register(meterRegistry)
                .increment();
    }

    /**
     * Registra um evento de validação de timestamp
     * 
     * @param valid true se o timestamp é válido, false caso contrário
     */
    public void recordTimestampValidation(boolean valid) {
        Counter.builder("webhook.timestamp.validation")
                .description("Total de validações de timestamp webhook")
                .tag("valid", String.valueOf(valid))
                .register(meterRegistry)
                .increment();
    }

    /**
     * Registra o atraso de um webhook (diferença entre criação e processamento)
     * 
     * @param delayMs Atraso em milissegundos
     */
    public void recordProcessingDelay(long delayMs) {
        Timer.builder("webhook.processing.delay")
                .description("Atraso entre a criação do webhook e seu processamento")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(delayMs));
    }

    /**
     * Incrementa o gauge de webhooks pendentes
     */
    public void incrementPending() {
        pendingWebhooks.incrementAndGet();
    }

    /**
     * Decrementa o gauge de webhooks pendentes
     */
    public void decrementPending() {
        pendingWebhooks.decrementAndGet();
    }
}
