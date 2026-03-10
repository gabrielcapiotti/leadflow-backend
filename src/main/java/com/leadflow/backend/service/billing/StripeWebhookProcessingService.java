package com.leadflow.backend.service.billing;

import com.leadflow.backend.entities.StripeEventLog;
import com.leadflow.backend.repository.StripeEventLogRepository;
import com.stripe.model.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class StripeWebhookProcessingService {

    private final StripeEventLogRepository eventLogRepository;
    private final StripeService stripeService;

    /**
     * Registra e processa um evento Stripe
     */
    @Transactional
    public void processAndLogEvent(Event event) {
        if (event == null || event.getId() == null) {
            log.warn("Received null or invalid event");
            return;
        }

        String eventId = event.getId();

        // Verificar se evento já foi processado
        Optional<StripeEventLog> existingEvent = eventLogRepository.findByEventId(eventId);
        if (existingEvent.isPresent()) {
            StripeEventLog eventLog = existingEvent.get();
            if (eventLog.getStatus() == StripeEventLog.EventProcessingStatus.SUCCESS) {
                log.info("Event already processed successfully: {}", eventId);
                return;
            }
            if (eventLog.getStatus() == StripeEventLog.EventProcessingStatus.FAILED) {
                log.warn("Event previously failed: {}", eventId);
                return;
            }
        }

        // Criar novo registro de evento
        StripeEventLog eventLog = StripeEventLog.builder()
            .eventId(eventId)
            .eventType(event.getType())
            .payload(event.toJson())
            .status(StripeEventLog.EventProcessingStatus.PENDING)
            .retryCount(0)
            .maxRetries(3)
            .build();

        eventLogRepository.save(eventLog);

        log.info("Event registered: {} (type: {})", eventId, event.getType());

        // Marca como processando
        processEvent(eventLog, event);
    }

    /**
     * Processa um evento com retry automático
     */
    @Transactional
    private void processEvent(StripeEventLog eventLog, Event event) {
        try {
            eventLog.setStatus(StripeEventLog.EventProcessingStatus.PROCESSING);
            eventLogRepository.save(eventLog);

            // Processar evento (delegado para outro serviço)
            stripeService.routeWebhookEvent(event);

            // Sucesso
            eventLog.setStatus(StripeEventLog.EventProcessingStatus.SUCCESS);
            eventLog.setProcessedAt(LocalDateTime.now());
            eventLogRepository.save(eventLog);

            log.info("Event processed successfully: {}", eventLog.getEventId());

        } catch (Exception e) {
            handleProcessingError(eventLog, e);
        }
    }

    /**
     * Trata erros de processamento com backoff exponencial
     */
    @Transactional
    private void handleProcessingError(StripeEventLog eventLog, Exception e) {
        eventLog.setRetryCount(eventLog.getRetryCount() + 1);
        eventLog.setLastError(e.getMessage());

        if (eventLog.getRetryCount() < eventLog.getMaxRetries()) {
            // Calcular próximo retry com backoff exponencial
            // 1º tentativa: 1 min, 2º: 2 min, 3º: 4 min
            long backoffMinutes = (long) Math.pow(2, eventLog.getRetryCount() - 1);
            LocalDateTime nextRetry = LocalDateTime.now().plusMinutes(backoffMinutes);

            eventLog.setStatus(StripeEventLog.EventProcessingStatus.RETRY_PENDING);
            eventLog.setNextRetryAt(nextRetry);

            log.warn("Processing failed for event {}. Retry scheduled for {} (attempt {}/{})",
                eventLog.getEventId(), nextRetry, eventLog.getRetryCount(), eventLog.getMaxRetries(), e);

        } else {
            eventLog.setStatus(StripeEventLog.EventProcessingStatus.FAILED);
            log.error("Processing permanently failed for event: {}", eventLog.getEventId(), e);
        }

        eventLogRepository.save(eventLog);
    }

    /**
     * Executa retry de eventos pendentes
     * Agendado para rodar a cada 5 minutos
     */
    @Scheduled(fixedRate = 300000) // 5 minutos
    @Transactional
    public void retryFailedEvents() {
        try {
            List<StripeEventLog> pendingRetries = eventLogRepository
                .findPendingRetries(StripeEventLog.EventProcessingStatus.RETRY_PENDING);

            if (pendingRetries.isEmpty()) {
                return;
            }

            log.info("Found {} events pending retry", pendingRetries.size());

            for (StripeEventLog eventLog : pendingRetries) {
                try {
                    // Resetar status para PENDING para que seja reprocessado
                    eventLog.setStatus(StripeEventLog.EventProcessingStatus.PENDING);
                    eventLog.setNextRetryAt(null);
                    eventLogRepository.save(eventLog);
                    
                    log.info("Event {} reset to PENDING for retry", eventLog.getEventId());
                } catch (Exception e) {
                    log.error("Error retrying event {}: {}", eventLog.getEventId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error during retry scheduled task", e);
        }
    }

    /**
     * Retorna estatísticas de eventos
     */
    public EventStatistics getEventStatistics() {
        return EventStatistics.builder()
            .totalProcessed(eventLogRepository.countByStatus(StripeEventLog.EventProcessingStatus.SUCCESS))
            .totalFailed(eventLogRepository.countByStatus(StripeEventLog.EventProcessingStatus.FAILED))
            .totalPending(eventLogRepository.countByStatus(StripeEventLog.EventProcessingStatus.PENDING))
            .totalRetryPending(eventLogRepository.countByStatus(StripeEventLog.EventProcessingStatus.RETRY_PENDING))
            .build();
    }

    // DTO para estatísticas
    @lombok.Data
    @lombok.Builder
    public static class EventStatistics {
        private long totalProcessed;
        private long totalFailed;
        private long totalPending;
        private long totalRetryPending;
    }
}
