package com.leadflow.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler global para exceções relacionadas a billing e assinaturas.
 * 
 * Retorna respostas padronizadas com HTTP status apropriados.
 */
@RestControllerAdvice
@Slf4j
public class BillingExceptionHandler {

    /**
     * Trata SubscriptionInactiveException
     * Retorna HTTP 402 PAYMENT_REQUIRED
     */
    @ExceptionHandler(SubscriptionInactiveException.class)
    public ResponseEntity<Map<String, Object>> handleSubscriptionInactive(
            SubscriptionInactiveException ex) {

        log.warn("Subscription inactive error: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("error", ex.getErrorCode());
        response.put("message", ex.getMessage());
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.PAYMENT_REQUIRED.value());

        return ResponseEntity
                .status(HttpStatus.PAYMENT_REQUIRED)
                .body(response);
    }

}
