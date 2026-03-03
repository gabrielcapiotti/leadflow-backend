package com.leadflow.backend.repository;

import com.leadflow.backend.entities.vendor.VendorLead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorLeadRepository extends JpaRepository<VendorLead, UUID> {

    List<VendorLead> findByVendorId(UUID vendorId);

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

    Optional<VendorLead> findFirstByVendorIdAndWhatsappOrderByCreatedDateDesc(
            UUID vendorId,
            String whatsapp
    );
}