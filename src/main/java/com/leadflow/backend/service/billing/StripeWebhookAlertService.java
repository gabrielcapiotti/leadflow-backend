package com.leadflow.backend.service.billing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Serviço de alertas para webhooks Stripe.
 * 
 * Monitora falhas de processamento de webhooks e envia alertas
 * quando um limite de falhas é atingido.
 * 
 * Nota: JavaMailSender é injetado como opcional para suportar
 * ambientes de teste que não têm email configurado.
 */
@Service
@Slf4j
public class StripeWebhookAlertService {

    private static final int DEFAULT_FAILURE_THRESHOLD = 5;
    private static final long ALERT_COOLDOWN_MS = 3600000; // 1 hora
    private static final int WINDOW_SIZE_MS = 300000; // 5 minutos

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${stripe.webhook.alert-threshold:5}")
    private int failureThreshold;

    @Value("${stripe.webhook.alert-email:}")
    private String alertEmail;

    @Value("${email.from:no-reply@leadflow.com}")
    private String emailFrom;

    @Value("${email.from-name:LeadFlow}")
    private String emailFromName;

    // Map para rastrear falhas recentes por tipo de evento
    private final ConcurrentHashMap<String, FailureWindow> failureWindows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> lastAlertTime = new ConcurrentHashMap<>();

    /**
     * Registra uma falha de processamento e verifica se deve enviar um alerta
     * 
     * @param eventType Tipo do evento que falhou
     * @param errorMessage Mensagem de erro
     * @param exception Exceção que ocorreu (opcional)
     */
    public void recordFailure(String eventType, String errorMessage, Throwable exception) {
        try {
            FailureWindow window = failureWindows.computeIfAbsent(
                    eventType,
                    k -> new FailureWindow(WINDOW_SIZE_MS)
            );

            int failureCount = window.recordFailure();

            log.warn("Webhook failure recorded for event type: {} (count: {} in last {}ms)",
                    eventType,
                    failureCount,
                    WINDOW_SIZE_MS,
                    exception);

            // Verifica se deve enviar alerta
            if (failureCount >= failureThreshold && shouldSendAlert(eventType)) {
                sendAlert(eventType, failureCount, errorMessage);
                lastAlertTime.put(eventType, LocalDateTime.now());
            }
        } catch (Exception e) {
            log.error("Error recording webhook failure", e);
        }
    }

    /**
     * Registra um sucesso (limpa contador se não houver mais falhas)
     * 
     * @param eventType Tipo do evento que teve sucesso
     */
    public void recordSuccess(String eventType) {
        FailureWindow window = failureWindows.get(eventType);
        if (window != null) {
            window.recordSuccess();
        }
    }

    /**
     * Obtém o número de falhas recentes para um tipo de evento
     * 
     * @param eventType Tipo do evento
     * @return Número de falhas nos últimos 5 minutos
     */
    public int getRecentFailureCount(String eventType) {
        FailureWindow window = failureWindows.get(eventType);
        return window != null ? window.getFailureCount() : 0;
    }

    /**
     * Verifica se deve enviar alerta baseado no tempo de cooldown
     */
    private boolean shouldSendAlert(String eventType) {
        LocalDateTime lastAlert = lastAlertTime.get(eventType);
        if (lastAlert == null) {
            return true;
        }
        long elapsed = System.currentTimeMillis() - lastAlert.getSecond() * 1000;
        return elapsed >= ALERT_COOLDOWN_MS;
    }

    /**
     * Envia alerta por log e email
     */
    private void sendAlert(String eventType, int failureCount, String errorMessage) {
        String alertMessage = String.format(
                "⚠️ WEBHOOK ALERT: Event type '%s' has %d failures (threshold: %d). Last error: %s",
                eventType,
                failureCount,
                failureThreshold,
                errorMessage
        );

        log.error(alertMessage);

        // Send email alert if configured and mail sender is available
        if (mailSender != null && alertEmail != null && !alertEmail.isBlank()) {
            try {
                sendAlertEmail(eventType, failureCount, errorMessage);
            } catch (Exception e) {
                log.error("Failed to send webhook alert email", e);
            }
        }
    }

    /**
     * Envia email de alerta para o administrador
     */
    private void sendAlertEmail(String eventType, int failureCount, String errorMessage) 
            throws MessagingException, java.io.UnsupportedEncodingException {
        
        if (mailSender == null) {
            log.warn("JavaMailSender not available, skipping email alert");
            return;
        }

        String subject = String.format("🚨 Alerta: %d falhas em webhooks Stripe (%s)", failureCount, eventType);
        String htmlContent = buildAlertEmailContent(eventType, failureCount, errorMessage);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(emailFrom, emailFromName);
        helper.setTo(alertEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
        log.info("Alert email sent to {} about webhook failures for event type: {}", alertEmail, eventType);
    }

    /**
     * Constrói o conteúdo HTML do email de alerta
     */
    private String buildAlertEmailContent(String eventType, int failureCount, String errorMessage) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%); color: white; padding: 30px 20px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; }
                    .content { padding: 30px 20px; }
                    .alert-box { background: #fadbd8; border-left: 4px solid #e74c3c; padding: 15px; border-radius: 4px; margin: 20px 0; }
                    .details { background: #f9f9f9; padding: 15px; border-radius: 4px; margin: 20px 0; border: 1px solid #ddd; font-family: monospace; font-size: 12px; word-break: break-all; }
                    .button { display: inline-block; padding: 10px 20px; background: #667eea; color: white; text-decoration: none; border-radius: 4px; margin-top: 15px; }
                    .footer { background: #f0f0f0; padding: 20px; text-align: center; font-size: 12px; color: #666; border-top: 1px solid #ddd; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🚨 Alerta de Falhas em Webhooks</h1>
                    </div>
                    <div class="content">
                        <p>Olá Administrador,</p>
                        <div class="alert-box">
                            <p><strong>Detectadas %d falhas consecutivas no processamento de webhooks Stripe!</strong></p>
                            <p><strong>Tipo de Evento:</strong> %s</p>
                            <p>Por favor, investigue imediatamente.</p>
                        </div>
                        <h3>Detalhes do Último Erro:</h3>
                        <div class="details">
                            %s
                        </div>
                        <h3>Ações Recomendadas:</h3>
                        <ul>
                            <li>Verifique o status da aplicação nos logs</li>
                            <li>Verifique a conexão com o banco de dados</li>
                            <li>Verifique se o serviço de email está funcionando</li>
                            <li>Verifique a configuração de SMTP</li>
                            <li>Considere pausar webhooks no painel Stripe até resolver</li>
                        </ul>
                        <p><strong>Monitoramento em Tempo Real:</strong> Acesse <a href="http://localhost:8080/actuator/prometheus">Prometheus Metrics</a> para visualizar métricas.</p>
                        <p><strong>Hora do Alerta:</strong> %s</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2026 LeadFlow. Sistema Automático de Alertas.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                failureCount,
                eventType,
                errorMessage,
                LocalDateTime.now().format(formatter)
            );
    }

    /**
     * Classe interna para rastrear falhas em uma janela de tempo
     */
    private static class FailureWindow {
        private final long windowSizeMs;
        private long lastResetTime;
        private final AtomicInteger failureCount;

        FailureWindow(long windowSizeMs) {
            this.windowSizeMs = windowSizeMs;
            this.lastResetTime = System.currentTimeMillis();
            this.failureCount = new AtomicInteger(0);
        }

        int recordFailure() {
            checkAndResetWindow();
            return failureCount.incrementAndGet();
        }

        void recordSuccess() {
            checkAndResetWindow();
        }

        int getFailureCount() {
            checkAndResetWindow();
            return failureCount.get();
        }

        private void checkAndResetWindow() {
            long now = System.currentTimeMillis();
            if (now - lastResetTime >= windowSizeMs) {
                failureCount.set(0);
                lastResetTime = now;
            }
        }
    }
}
