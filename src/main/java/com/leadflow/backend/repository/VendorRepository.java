package com.leadflow.backend.repository;

import com.leadflow.backend.entities.vendor.SubscriptionStatus;
import com.leadflow.backend.entities.vendor.Vendor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    /* ======================================================
       BASIC LOOKUPS
       ====================================================== */

    List<Vendor> findByUserEmail(String email);

    Optional<Vendor> findBySlug(String slug);

    List<Vendor> findBySubscriptionStatus(SubscriptionStatus status);

    List<Vendor> findBySubscriptionStatusIn(List<SubscriptionStatus> statuses);

    long countBySubscriptionStatus(SubscriptionStatus status);

    /* ======================================================
       GLOBAL QUERIES (PUBLIC SCHEMA)
       ====================================================== */

    @Query(value = "SELECT COUNT(*) FROM public.vendors", nativeQuery = true)
    long countAllGlobal();

    @Query(value = """
        SELECT COUNT(*)
        FROM public.vendors
        WHERE subscription_status = 'ATIVA'
        """, nativeQuery = true)
    long countActiveSubscriptionsGlobal();

    @Query(value = """
        SELECT COUNT(*)
        FROM public.vendors
        WHERE subscription_status = :status
        """, nativeQuery = true)
    long countBySubscriptionStatusGlobal(
            @Param("status") SubscriptionStatus status
    );

    @Query(value = """
        SELECT *
        FROM public.vendors
        WHERE subscription_started_at IS NOT NULL
        """, nativeQuery = true)
    List<Vendor> findAllWithSubscriptionStart();

    @Query(value = """
        SELECT DATE(v.created_at) AS day, COUNT(*) AS total
        FROM public.vendors v
        WHERE v.created_at >= :since
        GROUP BY DATE(v.created_at)
        ORDER BY DATE(v.created_at)
        """, nativeQuery = true)
    List<Object[]> countVendorsPerDayGlobal(
            @Param("since") Instant since
    );

    /* ======================================================
       PROJECTION
       ====================================================== */

    @Query("""
        SELECT v.id
        FROM Vendor v
        WHERE v.userEmail = :email
        """)
    Optional<UUID> findIdByUserEmail(@Param("email") String email);

}