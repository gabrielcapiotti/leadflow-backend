package com.leadflow.backend.repository;

import com.leadflow.backend.entities.vendor.Vendor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    List<Vendor> findByUserEmail(String userEmail);

    Optional<Vendor> findBySlug(String slug);
}