package com.leadflow.backend.repository;

import com.leadflow.backend.entities.StripeEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StripeEventLogRepository extends JpaRepository<StripeEventLog, Long> {

    Optional<StripeEventLog> findByEventId(String eventId);

    @Query("SELECT s FROM StripeEventLog s WHERE s.status = :status " +
           "AND s.nextRetryAt IS NOT NULL AND s.nextRetryAt <= CURRENT_TIMESTAMP " +
           "ORDER BY s.nextRetryAt ASC LIMIT 10")
    List<StripeEventLog> findPendingRetries(@Param("status") StripeEventLog.EventProcessingStatus status);

    @Query("SELECT s FROM StripeEventLog s WHERE s.status IN :statuses " +
           "ORDER BY s.createdAt DESC")
    List<StripeEventLog> findByStatuses(@Param("statuses") List<StripeEventLog.EventProcessingStatus> statuses);

    @Query("SELECT COUNT(s) FROM StripeEventLog s WHERE s.status = :status")
    long countByStatus(@Param("status") StripeEventLog.EventProcessingStatus status);

    @Query("SELECT COUNT(s) FROM StripeEventLog s WHERE s.eventType = :eventType " +
           "AND s.status = :status AND s.createdAt >= :since")
    long countByEventTypeAndStatusSince(
        @Param("eventType") String eventType,
        @Param("status") StripeEventLog.EventProcessingStatus status,
        @Param("since") LocalDateTime since
    );
}
