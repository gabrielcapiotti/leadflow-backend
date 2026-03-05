package com.leadflow.backend.repository;

import com.leadflow.backend.entities.vendor.VendorLead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorLeadRepository extends JpaRepository<VendorLead, UUID> {

    @Query(value = "SELECT COUNT(*) FROM public.vendor_leads", nativeQuery = true)
    long countAllGlobal();

        @Query(value = """
            SELECT DATE(l.created_date) AS day, COUNT(*) AS total
            FROM public.vendor_leads l
            WHERE l.created_date >= :since
            GROUP BY DATE(l.created_date)
            ORDER BY DATE(l.created_date)
            """, nativeQuery = true)
        List<Object[]> countLeadsPerDayGlobal(@Param("since") Instant since);

    List<VendorLead> findByVendorId(UUID vendorId);

    Page<VendorLead> findByVendorId(UUID vendorId, Pageable pageable);

    List<VendorLead> findByVendorIdOrderByScoreDesc(UUID vendorId);

    @Query("""
           SELECT v.stage, COUNT(v)
           FROM VendorLead v
           WHERE v.vendorId = :vendorId
           GROUP BY v.stage
           """)
    List<Object[]> countByStage(@Param("vendorId") UUID vendorId);

    List<VendorLead> findByOwnerEmailOrderByScoreDesc(String ownerEmail);

    Optional<VendorLead> findByIdAndVendorId(UUID id, UUID vendorId);

        @Query(value = """
            SELECT COUNT(*)
            FROM public.vendor_leads l
            WHERE l.vendor_id = :vendorId
              AND l.created_date >= :since
            """, nativeQuery = true)
        long countLast30Days(@Param("vendorId") UUID vendorId,
                 @Param("since") Instant since);

    Optional<VendorLead> findFirstByVendorIdAndWhatsappOrderByCreatedDateDesc(
            UUID vendorId,
            String whatsapp
    );
}