package com.leadflow.backend.service.notification;

import com.leadflow.backend.entities.vendor.EmailEvent;
import com.leadflow.backend.repository.EmailEventRepository;
import com.leadflow.backend.repository.VendorRepository;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;

@Service
public class SendGridEmailService {

    private static final Logger log = LoggerFactory.getLogger(SendGridEmailService.class);

    private final VendorRepository vendorRepository;
    private final EmailEventRepository emailEventRepository;

    public SendGridEmailService(VendorRepository vendorRepository,
                                EmailEventRepository emailEventRepository) {
        this.vendorRepository = vendorRepository;
        this.emailEventRepository = emailEventRepository;
    }

    @Value("${sendgrid.api-key}")
    private String apiKey;

    @Value("${sendgrid.from-email}")
    private String fromEmail;

    @Value("${sendgrid.retry.worker.max-attempts:8}")
    private int workerMaxAttempts;

    @Value("${sendgrid.retry.worker.delay-seconds:60}")
    private long workerDelaySeconds;

    @Retryable(
            retryFor = RetryableEmailException.class,
            maxAttemptsExpression = "#{${sendgrid.retry.max-attempts:3}}",
            backoff = @Backoff(
                    delayExpression = "#{${sendgrid.retry.initial-backoff-ms:300}}",
                    multiplierExpression = "#{${sendgrid.retry.multiplier:2.0}}"
            )
    )
    public void sendEmail(String to,
                          String subject,
                          String htmlContent) {
        sendEmailInternal(to, subject, htmlContent);
    }

    public void sendEmailWithoutQueue(String to,
                                      String subject,
                                      String htmlContent) {
        sendEmailInternal(to, subject, htmlContent);
    }

    private void sendEmailInternal(String to,
                                   String subject,
                                   String htmlContent) {

        boolean emailInvalid = vendorRepository.findByUserEmail(to)
            .stream()
            .anyMatch(vendor -> vendor.isEmailInvalid());

        if (emailInvalid) {
            log.warn("event=email_not_sent_invalid_recipient provider=sendgrid to={} subject={}",
                to, subject);
            return;
        }

        Email from = new Email(fromEmail);
        Email recipient = new Email(to);
        Content content = new Content("text/html", htmlContent);

        Mail mail = new Mail(from, subject, recipient, content);
        SendGrid sendGrid = new SendGrid(apiKey);

        String payload;
        try {
            payload = mail.build();
        } catch (IOException ex) {
            throw new NonRetryableEmailException("Falha ao montar payload de e-mail", ex);
        }

        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(payload);

        try {
            Response response = sendGrid.api(request);
            int status = response.getStatusCode();

            if (status < 400) {
                log.info("event=email_sent provider=sendgrid to={} subject={} status={}",
                        to, subject, status);
                return;
            }

            String sanitizedBody = sanitizeResponseBody(response.getBody());
            boolean retryable = isRetryableStatus(status);

            log.warn("event=email_send_failed provider=sendgrid to={} subject={} status={} retryable={} body={}",
                    to, subject, status, retryable, sanitizedBody);

            if (retryable) {
                throw new RetryableEmailException("Erro SendGrid (status " + status + "): " + sanitizedBody);
            }

            throw new NonRetryableEmailException("Erro SendGrid (status " + status + "): " + sanitizedBody);
        } catch (IOException ex) {
            throw new RetryableEmailException("Falha ao enviar e-mail", ex);
        }
    }

    @Recover
    public void recoverEmail(RetryableEmailException ex,
                             String to,
                             String subject,
                             String htmlContent) {
        log.error("event=email_retry_exhausted provider=sendgrid to={} subject={}", to, subject, ex);
        queueRetryEvent(to, subject, htmlContent, ex.getMessage());
        throw ex;
    }

    private void queueRetryEvent(String to,
                                 String subject,
                                 String htmlContent,
                                 String reason) {

        EmailEvent event = new EmailEvent();
        event.setEmail(to);
        event.setEventType("OUTBOUND_RETRY");
        event.setOccurredAt(Instant.now());
        event.setSubject(subject);
        event.setHtmlContent(htmlContent);
        event.setStatus("PENDING");
        event.setAttemptCount(0);
        event.setMaxAttempts(Math.max(workerMaxAttempts, 1));
        event.setNextRetryAt(Instant.now().plusSeconds(Math.max(workerDelaySeconds, 10L)));
        event.setReason(truncateReason(reason));

        emailEventRepository.save(event);
    }

    private String truncateReason(String reason) {
        if (reason == null) {
            return null;
        }

        int max = 1000;
        if (reason.length() <= max) {
            return reason;
        }

        return reason.substring(0, max);
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }

    private String sanitizeResponseBody(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "empty";
        }

        String compact = responseBody.replaceAll("\\s+", " ").trim();
        int maxLength = 400;

        if (compact.length() > maxLength) {
            return compact.substring(0, maxLength) + "...";
        }

        return compact;
    }

    private static class RetryableEmailException extends RuntimeException {
        private RetryableEmailException(String message) {
            super(message);
        }

        private RetryableEmailException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static class NonRetryableEmailException extends RuntimeException {
        private NonRetryableEmailException(String message) {
            super(message);
        }

        private NonRetryableEmailException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
