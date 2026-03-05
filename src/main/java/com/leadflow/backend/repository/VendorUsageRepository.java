package com.leadflow.backend.repository;

import com.leadflow.backend.entities.vendor.QuotaType;
import com.leadflow.backend.entities.vendor.VendorUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorUsageRepository extends JpaRepository<VendorUsage, UUID> {

    Optional<VendorUsage> findByVendorIdAndQuotaType(UUID vendorId, QuotaType quotaType);

    @Query("""
            SELECT COALESCE(SUM(v.used), 0)
            FROM VendorUsage v
            WHERE v.quotaType = :type
            """)
    long sumUsedByQuotaType(@Param("type") QuotaType type);

    @Query(value = "SELECT COALESCE(SUM(v.used), 0) FROM public.vendor_usage v WHERE v.quota_type = :type", nativeQuery = true)
    long sumUsedByQuotaTypeGlobal(@Param("type") String type);

        @Query(value = """
                        SELECT DATE(v.period_start) AS day, COALESCE(SUM(v.used), 0) AS total
                        FROM public.vendor_usage v
                        WHERE v.quota_type = :type
                            AND v.period_start >= :since
                        GROUP BY DATE(v.period_start)
                        ORDER BY DATE(v.period_start)
                        """, nativeQuery = true)
        List<Object[]> sumUsagePerDayGlobal(@Param("type") String type,
                                                                                @Param("since") Instant since);

        @Query(value = """
            SELECT COALESCE(SUM(v.used), 0)
            FROM public.vendor_usage v
            WHERE v.vendor_id = :vendorId
              AND v.quota_type = :type
              AND v.period_start >= :since
            """, nativeQuery = true)
        long sumLast30Days(@Param("vendorId") UUID vendorId,
                   @Param("type") String type,
                   @Param("since") Instant since);

        @Query(value = """
            SELECT MAX(v.period_start)
            FROM public.vendor_usage v
            WHERE v.vendor_id = :vendorId
            """, nativeQuery = true)
        Instant lastActivity(@Param("vendorId") UUID vendorId);
}
