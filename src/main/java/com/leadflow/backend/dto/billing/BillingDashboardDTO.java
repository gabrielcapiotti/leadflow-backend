package com.leadflow.backend.dto.billing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingDashboardDTO {

    private SubscriptionDetailsDTO subscription;
    private EventStatisticsDTO eventStatistics;
    private UsageStatisticsDTO usageStatistics;
    private Boolean hasActiveSubscription;
    private String currentStatus;
    private String nextAction;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EventStatisticsDTO {
        private Long totalProcessed;
        private Long totalFailed;
        private Long totalPending;
        private Long totalRetryPending;
        private Double successRate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UsageStatisticsDTO {
        private Long leadsCreated;
        private Long leadsLimit;
        private Double usagePercentage;
        private String usageStatus;
    }
}
