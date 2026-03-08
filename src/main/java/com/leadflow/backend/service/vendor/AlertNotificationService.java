package com.leadflow.backend.service.vendor;

import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class AlertNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public AlertNotificationService(@Nullable SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Envia uma notificação para um usuário específico via WebSocket.
     * Caso o WebSocket não esteja habilitado (ex: testes), a operação é ignorada.
     */
    public void notifyUser(String userEmail, Object payload) {

        if (messagingTemplate == null) {
            // WebSocket não disponível (ex: ambiente de teste)
            return;
        }

        if (userEmail == null || userEmail.isBlank() || payload == null) {
            return;
        }

        String safeUserEmail = Objects.requireNonNull(userEmail);
        Object safePayload = Objects.requireNonNull(payload);

        messagingTemplate.convertAndSendToUser(
                safeUserEmail,
                "/queue/alerts",
                safePayload
        );
    }
}