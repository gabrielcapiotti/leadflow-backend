package com.leadflow.backend.service.monitoring;

import com.leadflow.backend.service.notification.SendGridEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SystemAlertService {

    private static final Logger log = LoggerFactory.getLogger(SystemAlertService.class);

    private final SendGridEmailService emailService;

    @Value("${system.admin.email:}")
    private String adminEmail;

    public SystemAlertService(SendGridEmailService emailService) {
        this.emailService = emailService;
    }

    public void sendCriticalAlert(String message) {

        if (adminEmail == null || adminEmail.isBlank()) {
            log.warn("System critical alert skipped because system.admin.email is not configured: {}", message);
            return;
        }

        String html = """
            <h2>Alerta crítico do sistema</h2>
            <p>%s</p>
        """.formatted(message);

        try {
            emailService.sendEmail(
                    adminEmail,
                    "🚨 Alerta Leadflow AI",
                    html
            );
        } catch (Exception ex) {
            log.error("Failed to dispatch system critical alert email", ex);
        }
    }
}
