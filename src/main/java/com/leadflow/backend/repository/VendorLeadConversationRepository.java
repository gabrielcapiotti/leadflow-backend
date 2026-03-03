package com.leadflow.backend.repository;

import com.leadflow.backend.entities.vendor.VendorLeadConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VendorLeadConversationRepository
    extends JpaRepository<VendorLeadConversation, UUID> {

    List<VendorLeadConversation> findByVendorLeadIdOrderByCreatedAtAsc(UUID vendorLeadId);
}