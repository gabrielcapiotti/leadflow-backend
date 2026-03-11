package com.leadflow.backend.service.notification;

import com.leadflow.backend.entities.Subscription;
import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.repository.tenant.TenantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

/**
 * Serviço de notificação para assinaturas.
 * 
 * Cria e envia emails Thymeleaf em ocasiões específicas:
 * - Expiration reminder: 7 dias antes da expiração
 * - Cancellation notification: quando a assinatura é cancelada
 * - Payment failed notification: quando um pagamento falha
 * 
 * Nota: JavaMailSender é injetado como opcional para suportar
 * ambientes de teste que não têm email configurado.
 */
@Service
@Slf4j
public class SubscriptionNotificationService {

    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy");

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired(required = false)
    private TemplateEngine templateEngine;

    private final TenantRepository tenantRepository;

    @Value("${email.from}")
    private String emailFrom;

    @Value("${email.from-name}")
    private String emailFromName;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public SubscriptionNotificationService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /**
     * Envia email de aviso de expiração próxima (7 dias antes)
     */
    public void sendExpirationReminder(Subscription subscription) {
        if (mailSender == null || templateEngine == null) {
            log.debug("Email service not configured, skipping expiration reminder");
            return;
        }

        try {
            // Use the email from the subscription itself
            String email = subscription.getEmail();
            if (email == null || email.isBlank()) {
                log.warn("No email found for subscription {}", subscription.getId());
                return;
            }

            // Get tenant name for personalization
            Optional<Tenant> tenant = tenantRepository.findById(Objects.requireNonNull(subscription.getTenantId()));
            String tenantName = tenant.map(Tenant::getName).orElse("Valued Customer");

            Context context = new Context();
            context.setVariable("tenantName", tenantName);
            context.setVariable("expirationDate", subscription.getExpiresAt().format(DISPLAY_DATE_FORMATTER));
            context.setVariable("availableLeads", subscription.getPlan() != null ? subscription.getPlan().getMaxLeads() : 0);
            context.setVariable("renewalUrl", frontendUrl + "/billing/renew");

            String htmlContent = templateEngine.process("email/expiration-reminder", context);

            sendEmailWithTemplate(
                email,
                "⏰ Sua assinatura LeadFlow expirará em breve",
                htmlContent
            );

            log.info("Expiration reminder sent to {} (subscription {})", email, subscription.getId());
        } catch (Exception e) {
            log.error("Error sending expiration reminder for subscription {}", subscription.getId(), e);
        }
    }

    /**
     * Envia email notificando que a assinatura foi cancelada
     */
    public void sendCancellationNotification(Subscription subscription) {
        if (mailSender == null || templateEngine == null) {
            log.debug("Email service not configured, skipping cancellation notification");
            return;
        }

        try {
            // Use the email from the subscription itself
            String email = subscription.getEmail();
            if (email == null || email.isBlank()) {
                log.warn("No email found for subscription {}", subscription.getId());
                return;
            }

            // Get tenant name for personalization
            Optional<Tenant> tenant = tenantRepository.findById(Objects.requireNonNull(subscription.getTenantId()));
            String tenantName = tenant.map(Tenant::getName).orElse("Valued Customer");

            Context context = new Context();
            context.setVariable("tenantName", tenantName);
            context.setVariable("cancellationDate", Objects.requireNonNull(subscription.getCancelledAt()).format(DISPLAY_DATE_FORMATTER));
            context.setVariable("reactivationUrl", frontendUrl + "/billing/reactivate");

            String htmlContent = templateEngine.process("email/cancellation-notification", context);

            sendEmailWithTemplate(
                email,
                "❌ Sua assinatura LeadFlow foi cancelada",
                htmlContent
            );

            log.info("Cancellation notification sent to {} (subscription {})", email, subscription.getId());
        } catch (Exception e) {
            log.error("Error sending cancellation notification for subscription {}", subscription.getId(), e);
        }
    }

    /**
     * Envia email notificando que o pagamento falhou
     */
    public void sendPaymentFailedNotification(Subscription subscription) {
        if (mailSender == null || templateEngine == null) {
            log.debug("Email service not configured, skipping payment failed notification");
            return;
        }

        try {
            // Use the email from the subscription itself
            String email = subscription.getEmail();
            if (email == null || email.isBlank()) {
                log.warn("No email found for subscription {}", subscription.getId());
                return;
            }

            // Get tenant name for personalization
            Optional<Tenant> tenant = tenantRepository.findById(Objects.requireNonNull(subscription.getTenantId()));
            String tenantName = tenant.map(Tenant::getName).orElse("Valued Customer");

            Context context = new Context();
            context.setVariable("tenantName", tenantName);
            context.setVariable("failureDate", subscription.getLastPaymentDate() != null 
                ? subscription.getLastPaymentDate().format(DISPLAY_DATE_FORMATTER) 
                : "data desconhecida");
            context.setVariable("failureReason", "Cartão de crédito recusado ou método de pagamento inválido");
            context.setVariable("amount", "$99.99"); // Placeholder - pricing not in entities
            context.setVariable("updatePaymentUrl", frontendUrl + "/billing/payment-method");
            context.setVariable("retryPaymentUrl", frontendUrl + "/billing/retry-payment");

            String htmlContent = templateEngine.process("email/payment-failed-notification", context);

            sendEmailWithTemplate(
                email,
                "⚠️ Falha no pagamento da sua assinatura LeadFlow",
                htmlContent
            );

            log.info("Payment failed notification sent to {} (subscription {})", email, subscription.getId());
        } catch (Exception e) {
            log.error("Error sending payment failed notification for subscription {}", subscription.getId(), e);
        }
    }

    /**
     * Envia email com conteúdo HTML renderizado via Thymeleaf
     * 
     * @param to Email destinatário
     * @param subject Assunto do email
     * @param htmlContent Conteúdo HTML renderizado
     */
    private void sendEmailWithTemplate(String to, String subject, String htmlContent) 
            throws MessagingException, java.io.UnsupportedEncodingException {
        if (mailSender == null) {
            log.debug("Email service not configured, cannot send email to {}", to);
            return;
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(Objects.requireNonNull(emailFrom), Objects.requireNonNull(emailFromName));
        helper.setTo(Objects.requireNonNull(to));
        helper.setSubject(Objects.requireNonNull(subject));
        helper.setText(Objects.requireNonNull(htmlContent), true); // true = é HTML

        mailSender.send(message);
    }
}
