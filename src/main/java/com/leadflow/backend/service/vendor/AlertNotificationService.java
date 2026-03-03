package com.leadflow.backend.service.vendor;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class AlertNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public AlertNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyUser(String userEmail, Object payload) {
        messagingTemplate.convertAndSendToUser(
                userEmail,
                "/queue/alerts",
                payload
        );
    }
}
