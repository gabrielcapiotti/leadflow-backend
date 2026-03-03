package com.leadflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.leadflow.backend.entities.vendor.VendorLead;

import java.util.List;
import java.util.UUID;

public interface LeadRepositoryVendor extends JpaRepository<VendorLead, UUID> {

    List<VendorLead> findByVendorId(UUID vendorId);
}