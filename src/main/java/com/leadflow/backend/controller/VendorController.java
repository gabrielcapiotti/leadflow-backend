package com.leadflow.backend.controller;

import com.leadflow.backend.entities.vendor.Vendor; // ← IMPORT CORRETO
import com.leadflow.backend.repository.VendorRepository;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/vendors")
public class VendorController {

    private final VendorRepository repository;

    public VendorController(VendorRepository repository) {
        this.repository = repository;
    }

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

    @PostMapping
    public Vendor create(@RequestBody Vendor vendor) {
        return repository.save(vendor);
    }

    @PutMapping("/{id}")
    public Vendor update(@PathVariable UUID id,
                         @RequestBody Vendor data) {

        Vendor vendor = repository.findById(id)
                .orElseThrow();

        vendor.setNomeVendedor(data.getNomeVendedor());
        vendor.setWhatsappVendedor(data.getWhatsappVendedor());
        vendor.setNomeEmpresa(data.getNomeEmpresa());
        vendor.setLogoUrl(data.getLogoUrl());
        vendor.setCorDestaque(data.getCorDestaque());
        vendor.setMensagemBoasVindas(data.getMensagemBoasVindas());
        vendor.setSlug(data.getSlug());
        vendor.setStatusAssinatura(data.getStatusAssinatura());

        return repository.save(vendor);
    }
}