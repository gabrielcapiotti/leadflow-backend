package com.leadflow.backend.repository;

import com.leadflow.backend.entities.vendor.SubscriptionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SubscriptionHistoryRepository
        extends JpaRepository<SubscriptionHistory, UUID> {

    List<SubscriptionHistory> findByVendorIdOrderByChangedAtDesc(UUID vendorId);

    @Query(value = """
        SELECT COUNT(*)
        FROM public.subscription_history s
        WHERE s.new_status = 'CANCELADA'
          AND s.changed_at >= :since
        """, nativeQuery = true)
    long countCancellationsSinceGlobal(@Param("since") Instant since);

    @Query(value = """
        SELECT COUNT(*)
        FROM public.subscription_history s
        WHERE s.previous_status = 'TRIAL'
          AND s.new_status = 'ATIVA'
          AND s.changed_at >= :since
        """, nativeQuery = true)
    long countTrialConversionsSinceGlobal(@Param("since") Instant since);
}
