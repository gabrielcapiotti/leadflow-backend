package com.leadflow.backend.controller;

import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.repository.VendorRepository;
import com.leadflow.backend.service.subscription.TrialService;

import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/vendors")
public class VendorController {

    private final VendorRepository repository;
    private final TrialService trialService;

    public VendorController(
            VendorRepository repository,
            TrialService trialService
    ) {
        this.repository =
                Objects.requireNonNull(repository, "VendorRepository must not be null");

        this.trialService =
                Objects.requireNonNull(trialService, "TrialService must not be null");
    }

    /* ======================================================
       FILTER
       ====================================================== */

    @GetMapping
    public List<Vendor> filter(
            @RequestParam(required = false) String user_email,
            @RequestParam(required = false) String slug
    ) {

        if (user_email != null) {
            return repository.findByUserEmail(user_email);
        }

        if (slug != null) {
            return repository.findBySlug(slug)
                    .map(List::of)
                    .orElse(List.of());
        }

        return List.of();
    }

    /* ======================================================
       CREATE
       ====================================================== */

    @PostMapping
    public Vendor create(
            @RequestBody @NonNull Vendor vendor
    ) {

        Vendor safeVendor =
                Objects.requireNonNull(vendor, "Vendor must not be null");

        trialService.initializeTrial(safeVendor);

        return repository.save(safeVendor);
    }

    /* ======================================================
       UPDATE
       ====================================================== */

    @PutMapping("/{id}")
    public Vendor update(
            @PathVariable @NonNull UUID id,
            @RequestBody @NonNull Vendor data
    ) {

        UUID safeId =
                Objects.requireNonNull(id, "Vendor id must not be null");

        Vendor safeData =
                Objects.requireNonNull(data, "Vendor data must not be null");

        Vendor vendor = repository.findById(safeId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Vendor not found: " + safeId)
                );

        vendor.setNomeVendedor(safeData.getNomeVendedor());
        vendor.setWhatsappVendedor(safeData.getWhatsappVendedor());
        vendor.setNomeEmpresa(safeData.getNomeEmpresa());
        vendor.setLogoUrl(safeData.getLogoUrl());
        vendor.setCorDestaque(safeData.getCorDestaque());
        vendor.setMensagemBoasVindas(safeData.getMensagemBoasVindas());
        vendor.setSlug(safeData.getSlug());

        if (safeData.getSubscriptionStatus() != null) {
            vendor.setSubscriptionStatus(safeData.getSubscriptionStatus());
        }

        return repository.save(vendor);
    }
}