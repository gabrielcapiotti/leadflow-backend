package com.leadflow.backend.repository;

import com.leadflow.backend.entities.SubscriptionAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionAuditRepository extends JpaRepository<SubscriptionAudit, Long> {

    List<SubscriptionAudit> findBySubscriptionIdOrderByCreatedAtDesc(Long subscriptionId);

    List<SubscriptionAudit> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

}
