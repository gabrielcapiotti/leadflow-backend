package com.leadflow.backend.service.subscription;

import com.leadflow.backend.entities.vendor.SubscriptionStatus;
import com.leadflow.backend.entities.vendor.Vendor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TrialService {

    @Value("${subscription.trial-days}")
    private int trialDays;

    public void initializeTrial(Vendor vendor) {

        Instant now = Instant.now();

        vendor.setSubscriptionStatus(SubscriptionStatus.TRIAL);
        vendor.setSubscriptionStartedAt(now);
        vendor.setSubscriptionExpiresAt(now.plusSeconds(trialDays * 86400L));
    }
}
