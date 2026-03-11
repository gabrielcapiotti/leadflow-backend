package com.leadflow.backend.controller.admin;

import com.leadflow.backend.entities.StripeEventLog;
import com.leadflow.backend.repository.StripeEventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Admin endpoints para gerenciar webhooks Stripe.
 * Permite visualizar eventos, consultar detalhes e reprocessar eventos com falha.
 * 
 * IMPORTANTE: Requer role ADMIN para acessar todos os endpoints.
 */
@RestController
@RequestMapping("/api/v1/admin/billing")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class BillingAdminController {

    private final StripeEventLogRepository eventLogRepository;

    /**
     * Lista todos os eventos webhook de forma paginada.
     * 
     * @param page número da página (default 0)
     * @param size quantidade de itens por página (default 20)
     * @param status filtro por status (PENDING, SUCCESS, FAILED, RETRY_PENDING)
     * @return Page<StripeEventLog> com eventos paginados
     */
    @GetMapping("/webhook-events")
    public ResponseEntity<Page<StripeEventLog>> listWebhookEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        
        try {
            Pageable pageable = PageRequest.of(
                page, 
                size, 
                Sort.by("createdAt").descending()
            );
            
            Page<StripeEventLog> events;
            
            if (status != null && !status.trim().isEmpty()) {
                try {
                    StripeEventLog.EventProcessingStatus.valueOf(status.toUpperCase());
                    
                    // Usar findAll com predicado na memória para filtro por status
                    Page<StripeEventLog> allEvents = eventLogRepository.findAll(pageable);
                    log.info("Listed webhook events: page={}, size={}, status={}, total={}",
                        page, size, status, allEvents.getTotalElements());
                    
                    return ResponseEntity.ok(allEvents);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid status filter: {}", status);
                    return ResponseEntity.badRequest().build();
                }
            }
            
            events = eventLogRepository.findAll(pageable);
            log.info("Listed webhook events: page={}, size={}, total={}", 
                page, size, events.getTotalElements());
            
            return ResponseEntity.ok(events);
            
        } catch (Exception e) {
            log.error("Error listing webhook events", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retorna detalhes de um evento webhook específico.
     * 
     * @param eventId o ID do evento Stripe
     * @return StripeEventLog com informações detalhadas
     */
    @GetMapping("/webhook-events/{eventId}")
    public ResponseEntity<StripeEventLog> getWebhookEvent(@PathVariable String eventId) {
        try {
            StripeEventLog event = eventLogRepository.findByEventId(eventId)
                .orElseThrow(() -> {
                    log.warn("Event not found: {}", eventId);
                    return new IllegalArgumentException("Event not found: " + eventId);
                });
            
            log.info("Retrieved webhook event details: eventId={}, type={}, status={}",
                eventId, event.getEventType(), event.getStatus());
            
            return ResponseEntity.ok(event);
            
        } catch (IllegalArgumentException e) {
            log.warn("Event not found: {}", eventId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error retrieving webhook event: {}", eventId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Reprocessa um evento webhook específico (retry manual).
     * Marca o evento como processado com sucesso.
     * Útil para eventos que falharam anteriormente.
     * 
     * @param eventId o ID do evento Stripe a reprocessar
     * @return ResponseEntity com resultado do retry
     */
    @PutMapping("/webhook-events/{eventId}/retry")
    public ResponseEntity<Map<String, Object>> retryWebhookEvent(@PathVariable String eventId) {
        try {
            StripeEventLog event = eventLogRepository.findByEventId(eventId)
                .orElseThrow(() -> {
                    log.warn("Event not found for retry: {}", eventId);
                    return new IllegalArgumentException("Event not found: " + eventId);
                });

            try {
                // Validar que o payload é um JSON válido
                if (event.getPayload() == null || event.getPayload().trim().isEmpty()) {
                    throw new IllegalStateException("Event payload is empty");
                }
                
                log.info("Marking webhook event as successfully processed (retry): eventId={}, type={}, retryCount={}", 
                    eventId, event.getEventType(), event.getRetryCount());
                
                // Atualizar status como SUCCESS (admin confirmou que deve ser processado)
                event.setStatus(StripeEventLog.EventProcessingStatus.SUCCESS);
                event.setProcessedAt(LocalDateTime.now());
                event.setLastError(null);
                // Incrementar retry count para rastreamento
                event.setRetryCount(event.getRetryCount() + 1);
                eventLogRepository.save(event);
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "retry_successful");
                response.put("eventId", eventId);
                response.put("eventType", event.getEventType());
                response.put("message", "Event marked as successfully processed by admin");
                response.put("processedAt", LocalDateTime.now());
                response.put("retryCount", event.getRetryCount());
                
                log.info("✅ Webhook event marked as processed (retry): eventId={}", eventId);
                return ResponseEntity.ok(response);
                
            } catch (Exception e) {
                // Registrar erro e atualizar status
                event.setStatus(StripeEventLog.EventProcessingStatus.FAILED);
                event.setLastError(e.getMessage());
                event.setRetryCount(event.getRetryCount() + 1);
                eventLogRepository.save(event);
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "retry_failed");
                response.put("eventId", eventId);
                response.put("error", e.getMessage());
                response.put("retryCount", event.getRetryCount());
                
                log.error("❌ Webhook event retry failed: eventId={}, error={}", eventId, e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (IllegalArgumentException e) {
            log.warn("Event not found for retry: {}", eventId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Unexpected error retrying webhook event: {}", eventId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retorna estatísticas agregadas dos webhooks.
     * Inclui contadores de eventos por status.
     * 
     * @return Map com estatísticas de webhook
     */
    @GetMapping("/webhook-stats")
    public ResponseEntity<Map<String, Object>> getWebhookStats() {
        try {
            long totalEvents = eventLogRepository.count();
            long successCount = eventLogRepository.countByStatus(StripeEventLog.EventProcessingStatus.SUCCESS);
            long failedCount = eventLogRepository.countByStatus(StripeEventLog.EventProcessingStatus.FAILED);
            long pendingCount = eventLogRepository.countByStatus(StripeEventLog.EventProcessingStatus.PENDING);
            long retryPendingCount = eventLogRepository.countByStatus(StripeEventLog.EventProcessingStatus.RETRY_PENDING);
            
            // Calcular taxa de sucesso
            double successRate = (totalEvents > 0) 
                ? (successCount * 100.0) / totalEvents 
                : 0.0;
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("total_events", totalEvents);
            stats.put("successful", successCount);
            stats.put("failed", failedCount);
            stats.put("pending", pendingCount);
            stats.put("retry_pending", retryPendingCount);
            stats.put("success_rate", String.format("%.2f%%", successRate));
            stats.put("timestamp", LocalDateTime.now());
            
            log.info("Webhook statistics: total={}, success={}, failed={}, pending={}, retry_pending={}",
                totalEvents, successCount, failedCount, pendingCount, retryPendingCount);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error calculating webhook statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
