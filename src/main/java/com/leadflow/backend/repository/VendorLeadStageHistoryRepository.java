package com.leadflow.backend.repository;

import com.leadflow.backend.entities.vendor.VendorLeadStageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface VendorLeadStageHistoryRepository
        extends JpaRepository<VendorLeadStageHistory, UUID> {

    List<VendorLeadStageHistory> findByVendorLeadIdOrderByChangedAtDesc(UUID vendorLeadId);

    @Query("""
           SELECT h.previousStage, h.newStage, COUNT(h)
           FROM VendorLeadStageHistory h
           JOIN VendorLead l ON l.id = h.vendorLeadId
           WHERE l.vendorId = :vendorId
           GROUP BY h.previousStage, h.newStage
           """)
    List<Object[]> countTransitionsByVendor(@Param("vendorId") UUID vendorId);
}