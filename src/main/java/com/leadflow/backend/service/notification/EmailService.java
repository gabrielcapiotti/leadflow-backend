package com.leadflow.backend.service.notification;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendHotLeadEmail(String to,
                                 String leadId,
                                 int score) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("🔥 Lead Quente Detectado");
        message.setText("""
                Um lead com alta probabilidade de fechamento foi detectado.

                Lead ID: %s
                Score: %d

                Acesse o sistema para agir imediatamente.
                """.formatted(leadId, score));

        mailSender.send(message);
    }
}
