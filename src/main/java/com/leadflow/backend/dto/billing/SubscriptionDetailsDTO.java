package com.leadflow.backend.dto.billing;

import com.leadflow.backend.entities.Subscription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionDetailsDTO {

    private Long subscriptionId;
    private String tenantId;
    private String stripeCustomerId;
    private String stripeSubscriptionId;
    private String status;
    private String planName;
    private Integer maxLeads;
    private Integer maxUsers;
    private Integer maxAiExecutions;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private Long daysUntilExpiration;
    private Boolean isActive;
    private Boolean isPastDue;
    private Boolean isCancelled;
    private LocalDateTime createdAt;

    public static SubscriptionDetailsDTO fromEntity(Subscription subscription) {
        boolean isActive = subscription.getStatus() == Subscription.SubscriptionStatus.ACTIVE;
        boolean isPastDue = subscription.getStatus() == Subscription.SubscriptionStatus.PAST_DUE;
        boolean isCancelled = subscription.getStatus() == Subscription.SubscriptionStatus.CANCELLED;

        LocalDateTime now = LocalDateTime.now();
        long daysUntil = java.time.temporal.ChronoUnit.DAYS
            .between(now, subscription.getExpiresAt());

        return SubscriptionDetailsDTO.builder()
            .subscriptionId(subscription.getId())
            .tenantId(subscription.getTenantId().toString())
            .stripeCustomerId(subscription.getStripeCustomerId())
            .stripeSubscriptionId(subscription.getStripeSubscriptionId())
            .status(subscription.getStatus().name())
            .planName(subscription.getPlan().getName())
            .maxLeads(subscription.getPlan().getMaxLeads())
            .maxUsers(subscription.getPlan().getMaxUsers())
            .maxAiExecutions(subscription.getPlan().getMaxAiExecutions())
            .startedAt(subscription.getStartedAt())
            .expiresAt(subscription.getExpiresAt())
            .daysUntilExpiration(daysUntil)
            .isActive(isActive)
            .isPastDue(isPastDue)
            .isCancelled(isCancelled)
            .createdAt(subscription.getCreatedAt())
            .build();
    }
}
