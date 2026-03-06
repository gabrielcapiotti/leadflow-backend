package com.leadflow.backend.security;

import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.repository.VendorRepository;
import com.leadflow.backend.security.exception.UnauthorizedException;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
public class VendorContext {

    private final VendorRepository vendorRepository;

    public VendorContext(VendorRepository vendorRepository) {
        this.vendorRepository = Objects.requireNonNull(
                vendorRepository,
                "vendorRepository cannot be null"
        );
    }

    /**
     * Retorna o Vendor associado ao usuário autenticado.
     *
     * Fluxo:
     * JWT Filter → SecurityContextHolder → VendorContext → VendorRepository
     */
    public Vendor getCurrentVendor() {

        Authentication authentication = getAuthentication();

        String email = authentication.getName();

        if (email == null || email.isBlank()) {
            throw new UnauthorizedException("Authenticated user email not found");
        }

        return vendorRepository
                .findByUserEmail(email)
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new UnauthorizedException(
                                "Authenticated user does not belong to any vendor"
                        ));
    }

    /**
     * Retorna apenas o ID do Vendor atual.
     * Evita carregar a entidade completa quando não necessário.
     */
    public UUID getCurrentVendorId() {
        return getCurrentVendor().getId();
    }

    /**
     * Obtém a autenticação atual com validações de segurança.
     */
    private Authentication getAuthentication() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new UnauthorizedException("No authentication found in security context");
        }

        if (!authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }

        if (authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("Anonymous authentication is not allowed");
        }

        return authentication;
    }
}