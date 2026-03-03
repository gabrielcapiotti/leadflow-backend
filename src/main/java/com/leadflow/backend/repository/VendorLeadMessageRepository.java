package com.leadflow.backend.repository;

import com.leadflow.backend.entities.vendor.VendorLeadMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VendorLeadMessageRepository extends JpaRepository<VendorLeadMessage, UUID> {

    List<VendorLeadMessage> findByVendorLeadIdOrderByCreatedAtAsc(UUID vendorLeadId);
}