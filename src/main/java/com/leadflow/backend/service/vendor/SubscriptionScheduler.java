package com.leadflow.backend.service.vendor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final SubscriptionService subscriptionService;

    /**
     * Executa diariamente às 00:00 (meia-noite)
     * para expirar assinaturas que atingiram a data de expiração
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void expireSubscriptionsDaily() {
        try {
            log.info("Starting scheduled subscription expiration check");
            subscriptionService.expireSubscriptions();
            log.info("Completed scheduled subscription expiration check");
        } catch (Exception e) {
            log.error("Error during scheduled subscription expiration", e);
        }
    }

    /**
     * Executa todos os dias às 09:00
     * para notificar sobre assinaturas que expiram em 7 dias
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void notifyExpiringSubscriptionsDaily() {
        try {
            log.info("Starting scheduled expiration notification check");
            subscriptionService.notifyExpiringSubscriptions();
            log.info("Completed scheduled expiration notification check");
        } catch (Exception e) {
            log.error("Error during scheduled expiration notification", e);
        }
    }
}
