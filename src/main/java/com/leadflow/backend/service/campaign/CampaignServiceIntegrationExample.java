package com.leadflow.backend.service.campaign;

import com.leadflow.backend.service.vendor.SubscriptionService;
import com.leadflow.backend.security.VendorContext;

import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Example of how to integrate subscription validation in CampaignService
 * This pattern should be applied to all resource-consuming operations
 */
@Service
public class CampaignServiceIntegrationExample {

    private final SubscriptionService subscriptionService;
    private final VendorContext vendorContext;

    public CampaignServiceIntegrationExample(
            SubscriptionService subscriptionService,
            VendorContext vendorContext
    ) {
        this.subscriptionService = subscriptionService;
        this.vendorContext = vendorContext;
    }

    /**
     * Example: Creating a campaign should validate subscription first
     */
    public void createCampaign(String name, String description) {
        // Get current tenant
        UUID tenantId = vendorContext.getCurrentVendorId();

        // STEP 1: Validate subscription is active
        // Throws SubscriptionInactiveException (HTTP 402) if not active
        subscriptionService.validateActiveSubscription(tenantId);

        // STEP 2: Check usage limits
        // usageService.validateCampaignQuota(tenantId);

        // STEP 3: Create the campaign
        // Campaign campaign = new Campaign(name, description);
        // campaignRepository.save(campaign);

        // STEP 4: Log if needed
        // auditService.logCampaignCreated(tenantId, campaign.getId());
    }

    /**
     * Example: Updating campaign settings should also validate
     */
    public void updateCampaignSettings(UUID campaignId, String setting, String value) {
        UUID tenantId = vendorContext.getCurrentVendorId();

        // Validate subscription before allowing updates
        subscriptionService.validateActiveSubscription(tenantId);

        // ... perform update ...
    }

    /**
     * Example: Launching campaign should validate
     */
    public void launchCampaign(UUID campaignId) {
        UUID tenantId = vendorContext.getCurrentVendorId();

        // Validate subscription before launching
        subscriptionService.validateActiveSubscription(tenantId);

        // ... launch campaign ...
    }
}
