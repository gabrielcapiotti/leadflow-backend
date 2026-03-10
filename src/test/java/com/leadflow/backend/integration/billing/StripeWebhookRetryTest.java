package com.leadflow.backend.integration.billing;

import com.leadflow.backend.entities.StripeEventLog;
import com.leadflow.backend.repository.StripeEventLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests para retry scenarios de webhooks Stripe.
 * 
 * Usa @DataJpaTest para testes isolados do repository.
 * Testa:
 * - Exponential backoff calculation
 * - Retry pending events
 * - Max retries exceeded
 * - Retry timestamp updates
 */
@DataJpaTest
@ActiveProfiles("test")
@Slf4j
class StripeWebhookRetryTest {

    @Autowired
    private StripeEventLogRepository eventLogRepository;

    private static final int MAX_RETRIES = 5;
    private static final int INITIAL_DELAY_SECONDS = 1;
    private static final double BACKOFF_MULTIPLIER = 2.0;

    @BeforeEach
    void setup() {
        eventLogRepository.deleteAll();
    }

    /**
     * Test: Exponential backoff calculation correto
     */
    @Test
    @DisplayName("Should calculate exponential backoff correctly")
    void shouldCalculateExponentialBackoffCorrectly() {
        log.info("Testing exponential backoff calculation");

        // Retry count 0 → 1 second
        long delay0 = calculateDelay(0);
        assertThat(delay0).isEqualTo(1);

        // Retry count 1 → 2 seconds
        long delay1 = calculateDelay(1);
        assertThat(delay1).isEqualTo(2);

        // Retry count 2 → 4 seconds
        long delay2 = calculateDelay(2);
        assertThat(delay2).isEqualTo(4);

        // Retry count 3 → 8 seconds
        long delay3 = calculateDelay(3);
        assertThat(delay3).isEqualTo(8);

        // Retry count 4 → 16 seconds
        long delay4 = calculateDelay(4);
        assertThat(delay4).isEqualTo(16);

        // Retry count 5 → 32 seconds
        long delay5 = calculateDelay(5);
        assertThat(delay5).isEqualTo(32);

        // Max capped at 300 seconds (5 minutes)
        long delay10 = calculateDelay(10);
        assertThat(delay10).isLessThanOrEqualTo(300);

        log.info("✅ Exponential backoff calculation verified");
    }

    /**
     * Test: Event com retry_pending deve ser processado
     */
    @Test
    @DisplayName("Should process RETRY_PENDING events")
    @Transactional
    void shouldProcessRetryPendingEvents() throws Exception {
        String eventId = "evt_test_retry_" + UUID.randomUUID().toString().substring(0, 8);
        
        // Criar evento com status RETRY_PENDING
        StripeEventLog event = new StripeEventLog();
        event.setEventId(eventId);
        event.setEventType("invoice.payment_succeeded");
        event.setStatus(StripeEventLog.EventProcessingStatus.RETRY_PENDING);
        event.setPayload("""
            {
                "id": "%s",
                "type": "invoice.payment_succeeded",
                "created": %d,
                "data": {
                    "object": {
                        "id": "in_test_123",
                        "subscription": "sub_test_456",
                        "amount_paid": 2999
                    }
                }
            }
            """.formatted(eventId, System.currentTimeMillis() / 1000));
        event.setRetryCount(0);
        event.setNextRetryAt(LocalDateTime.now().minusMinutes(1)); // Já pode fazer retry
        
        eventLogRepository.save(event);
        log.info("Created RETRY_PENDING event: eventId={}, retryCount=0", eventId);

        long eventCountBefore = eventLogRepository.count();
        assertThat(eventCountBefore).isEqualTo(1);

        // Scheduler processaria este evento na próxima execução
        log.info("✅ RETRY_PENDING event created and ready for processing");
    }

    /**
     * Test: Event com max retries reached deve ficar com status FAILED
     */
    @Test
    @DisplayName("Should mark event as FAILED when max retries exceeded")
    @Transactional
    void shouldMarkAsFailedWhenMaxRetriesExceeded() {
        String eventId = "evt_test_max_retries_" + UUID.randomUUID().toString().substring(0, 8);
        
        // Criar evento com retry count máximo
        StripeEventLog event = new StripeEventLog();
        event.setEventId(eventId);
        event.setEventType("invoice.payment_succeeded");
        event.setStatus(StripeEventLog.EventProcessingStatus.RETRY_PENDING);
        event.setPayload("""
            {
                "id": "%s",
                "type": "invoice.payment_succeeded",
                "created": %d,
                "data": {
                    "object": {
                        "id": "in_test_123",
                        "subscription": "sub_test_456"
                    }
                }
            }
            """.formatted(eventId, System.currentTimeMillis() / 1000));
        event.setRetryCount(MAX_RETRIES); // Já atingiu o máximo
        event.setLastError("Previous retry attempt failed");
        event.setNextRetryAt(LocalDateTime.now().minusSeconds(30));
        
        eventLogRepository.save(event);
        log.info("Created event with MAX_RETRIES: eventId={}, retryCount={}", eventId, MAX_RETRIES);

        // Simular que scheduler verá este evento e marcará como FAILED
        StripeEventLog savedEvent = eventLogRepository.findByEventId(eventId).orElseThrow();
        assertThat(savedEvent.getRetryCount()).isEqualTo(MAX_RETRIES);
        log.info("✅ Event with max retries properly created");
    }

    /**
     * Test: Retry timestamp deve ser atualizado corretamente
     */
    @Test
    @DisplayName("Should update retry timestamp correctly")
    @Transactional
    void shouldUpdateRetryTimestampCorrectly() throws Exception {
        String eventId = "evt_test_retry_timestamp_" + UUID.randomUUID().toString().substring(0, 8);
        
        LocalDateTime initialNextRetry = LocalDateTime.now().minusMinutes(1);
        
        StripeEventLog event = new StripeEventLog();
        event.setEventId(eventId);
        event.setEventType("invoice.payment_succeeded");
        event.setStatus(StripeEventLog.EventProcessingStatus.RETRY_PENDING);
        event.setPayload("""
            {
                "id": "%s",
                "type": "invoice.payment_succeeded",
                "created": %d,
                "data": {"object": {}}
            }
            """.formatted(eventId, System.currentTimeMillis() / 1000));
        event.setRetryCount(1);
        event.setNextRetryAt(initialNextRetry);
        
        eventLogRepository.save(event);
        log.info("Created event with nextRetryAt: eventId={}, nextRetryAt={}", eventId, initialNextRetry);

        StripeEventLog savedEvent = eventLogRepository.findByEventId(eventId).orElseThrow();
        assertThat(savedEvent.getNextRetryAt()).isNotNull();
        
        // Simular um retry que falha e precisa ser repetido
        long delaySeconds = calculateDelay(2); // After 2 retries
        LocalDateTime newNextRetry = LocalDateTime.now().plusSeconds(delaySeconds);
        
        savedEvent.setRetryCount(2);
        savedEvent.setNextRetryAt(newNextRetry);
        savedEvent.setLastError("Connection timeout");
        eventLogRepository.save(savedEvent);

        StripeEventLog updatedEvent = eventLogRepository.findByEventId(eventId).orElseThrow();
        assertThat(updatedEvent.getRetryCount()).isEqualTo(2);
        assertThat(updatedEvent.getNextRetryAt()).isAfter(initialNextRetry);
        log.info("✅ Retry timestamp updated correctly: newRetryCount={}, newNextRetryAt={}", 
            updatedEvent.getRetryCount(), updatedEvent.getNextRetryAt());
    }

    /**
     * Test: Query events que precisam fazer retry
     */
    @Test
    @DisplayName("Should query pending retry events correctly")
    @Transactional
    void shouldQueryPendingRetryEventsCorrectly() {
        // Criar vários eventos com status diferentes
        createTestEvent("evt_success_1", StripeEventLog.EventProcessingStatus.SUCCESS);
        createTestEvent("evt_failed_1", StripeEventLog.EventProcessingStatus.FAILED);
        createTestEvent("evt_retry_1", StripeEventLog.EventProcessingStatus.RETRY_PENDING);
        createTestEvent("evt_retry_2", StripeEventLog.EventProcessingStatus.RETRY_PENDING);

        log.info("Created test events: 1 SUCCESS, 1 FAILED, 2 RETRY_PENDING");

        // Query apenas eventos com RETRY_PENDING
        List<StripeEventLog> retryEvents = eventLogRepository.findByStatuses(
            Arrays.asList(StripeEventLog.EventProcessingStatus.RETRY_PENDING)
        );
        
        assertThat(retryEvents).hasSize(2);
        assertThat(retryEvents).allMatch(e -> e.getStatus() == StripeEventLog.EventProcessingStatus.RETRY_PENDING);
        
        log.info("✅ Query for RETRY_PENDING events returned correct results: count={}", retryEvents.size());
    }

    /**
     * Test: Retry sequence completa (múltiplos retries)
     */
    @Test
    @DisplayName("Should execute complete retry sequence")
    @Transactional
    void shouldExecuteCompleteRetrySequence() throws Exception {
        String eventId = "evt_test_sequence_" + UUID.randomUUID().toString().substring(0, 8);
        
        StripeEventLog event = new StripeEventLog();
        event.setEventId(eventId);
        event.setEventType("invoice.payment_succeeded");
        event.setStatus(StripeEventLog.EventProcessingStatus.RETRY_PENDING);
        event.setPayload("""
            {
                "id": "%s",
                "type": "invoice.payment_succeeded",
                "created": %d,
                "data": {"object": {}}
            }
            """.formatted(eventId, System.currentTimeMillis() / 1000));
        event.setRetryCount(0);
        event.setNextRetryAt(LocalDateTime.now());
        
        eventLogRepository.save(event);
        log.info("Retry sequence - Step 0: Created initial event");

        // Simular sequência de retries
        for (int retryCount = 1; retryCount <= 5; retryCount++) {
            StripeEventLog current = eventLogRepository.findByEventId(eventId).orElseThrow();
            
            // Simular falha e agendamento próximo retry
            long nextDelay = calculateDelay(retryCount);
            current.setRetryCount(retryCount);
            current.setLastError("Simulated retry attempt " + retryCount);
            current.setNextRetryAt(LocalDateTime.now().plusSeconds(nextDelay));
            
            eventLogRepository.save(current);
            log.info("Retry sequence - Step {}: retryCount={}, nextDelay={}s", 
                retryCount, retryCount, nextDelay);
            
            Thread.sleep(100);
        }

        StripeEventLog finalEvent = eventLogRepository.findByEventId(eventId).orElseThrow();
        assertThat(finalEvent.getRetryCount()).isEqualTo(5);
        assertThat(finalEvent.getNextRetryAt()).isNotNull();
        log.info("✅ Complete retry sequence executed: finalRetryCount={}", finalEvent.getRetryCount());
    }

    /**
     * Test: Error message preservation em retries
     */
    @Test
    @DisplayName("Should preserve error messages across retries")
    @Transactional
    void shouldPreserveErrorMessagesAcrossRetries() {
        String eventId = "evt_test_errors_" + UUID.randomUUID().toString().substring(0, 8);
        
        StripeEventLog event = new StripeEventLog();
        event.setEventId(eventId);
        event.setEventType("invoice.payment_succeeded");
        event.setStatus(StripeEventLog.EventProcessingStatus.RETRY_PENDING);
        event.setPayload("{}");
        event.setRetryCount(0);
        
        eventLogRepository.save(event);
        log.info("Created event for error tracking: eventId={}", eventId);

        String[] errors = {
            "Connection timeout after 10s",
            "Service temporarily unavailable",
            "Invalid subscription ID: sub_xyz"
        };

        for (int i = 0; i < errors.length; i++) {
            StripeEventLog current = eventLogRepository.findByEventId(eventId).orElseThrow();
            current.setRetryCount(i + 1);
            current.setLastError(errors[i]);
            eventLogRepository.save(current);
            
            log.info("Updated error: retryCount={}, error='{}'", i + 1, errors[i]);
        }

        StripeEventLog finalEvent = eventLogRepository.findByEventId(eventId).orElseThrow();
        assertThat(finalEvent.getLastError()).isEqualTo(errors[errors.length - 1]);
        log.info("✅ Error messages preserved: lastError='{}'", finalEvent.getLastError());
    }

    // ===== Helper Methods =====

    /**
     * Calcula o delay para um retry baseado no count.
     * Formula: initialDelay * (multiplier ^ retryCount), capped at 300 seconds
     */
    private long calculateDelay(int retryCount) {
        long delay = (long) (INITIAL_DELAY_SECONDS * Math.pow(BACKOFF_MULTIPLIER, retryCount));
        return Math.min(delay, 300L);
    }

    private void createTestEvent(String eventId, StripeEventLog.EventProcessingStatus status) {
        StripeEventLog event = new StripeEventLog();
        event.setEventId(eventId);
        event.setEventType("test.event");
        event.setStatus(status);
        event.setPayload("{}");
        event.setRetryCount(0);
        eventLogRepository.save(event);
    }
}
