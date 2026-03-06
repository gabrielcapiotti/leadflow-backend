package com.leadflow.backend.service.vendor;

import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

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

        messagingTemplate.convertAndSendToUser(
                userEmail,
                "/queue/alerts",
                payload
        );
    }
}