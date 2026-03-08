package com.leadflow.backend.service.notification;

import com.leadflow.backend.entities.vendor.EmailEvent;
import com.leadflow.backend.repository.EmailEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import java.time.Instant;
import java.util.List;

@Service
@Profile("!test")
public class EmailRetryWorker {

    private static final Logger log = LoggerFactory.getLogger(EmailRetryWorker.class);

    private final EmailEventRepository emailEventRepository;
    private final SendGridEmailService sendGridEmailService;

    @Value("${sendgrid.retry.worker.delay-seconds:60}")
    private long workerDelaySeconds;

    public EmailRetryWorker(
            EmailEventRepository emailEventRepository,
            SendGridEmailService sendGridEmailService
    ) {
        this.emailEventRepository = emailEventRepository;
        this.sendGridEmailService = sendGridEmailService;
    }

    @Scheduled(fixedDelayString = "${sendgrid.retry.worker.fixed-delay-ms:60000}")
    @Transactional
    public void processPendingRetries() {

        List<EmailEvent> pending =
                emailEventRepository
                        .findTop50ByStatusAndNextRetryAtLessThanEqualOrderByOccurredAtAsc(
                                "PENDING",
                                Instant.now()
                        );

        for (EmailEvent event : pending) {
            processSingleEvent(event);
        }
    }

    private void processSingleEvent(EmailEvent event) {

        int currentAttempts =
                event.getAttemptCount() == null ? 0 : event.getAttemptCount();

        int maxAttempts =
                event.getMaxAttempts() == null
                        ? 1
                        : Math.max(event.getMaxAttempts(), 1);

        try {

            sendGridEmailService.sendEmailWithoutQueue(
                    event.getEmail(),
                    event.getSubject(),
                    event.getHtmlContent()
            );

            event.setStatus("SENT");
            event.setAttemptCount(currentAttempts + 1);
            event.setProcessedAt(Instant.now());
            event.setNextRetryAt(null);
            event.setReason(null);

            emailEventRepository.save(event);

        } catch (RuntimeException ex) {

            int nextAttempt = currentAttempts + 1;

            event.setAttemptCount(nextAttempt);
            event.setReason(truncate(ex.getMessage()));

            if (nextAttempt >= maxAttempts) {

                event.setStatus("FAILED");
                event.setProcessedAt(Instant.now());
                event.setNextRetryAt(null);

            } else {

                event.setStatus("PENDING");

                long delaySeconds =
                        Math.max(workerDelaySeconds, 10L) * nextAttempt;

                event.setNextRetryAt(
                        Instant.now().plusSeconds(delaySeconds)
                );
            }

            emailEventRepository.save(event);

            log.warn(
                    "event=email_retry_failed id={} attempt={} maxAttempts={} status={}",
                    event.getId(),
                    nextAttempt,
                    maxAttempts,
                    event.getStatus()
            );
        }
    }

    private String truncate(String message) {

        if (message == null) {
            return null;
        }

        return message.length() <= 1000
                ? message
                : message.substring(0, 1000);
    }

    private class EmailRetryCallback implements RetryCallback<Void, RuntimeException> {
        @Override
        public Void doWithRetry(RetryContext context) {
            return null;
        }
    }
}