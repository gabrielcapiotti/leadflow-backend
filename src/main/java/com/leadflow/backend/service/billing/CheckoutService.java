package com.leadflow.backend.service.billing;

import com.leadflow.backend.dto.billing.CheckoutRequest;
import com.leadflow.backend.dto.billing.CheckoutResponse;
import com.leadflow.backend.entities.billing.PaymentCheckoutRequest;
import com.leadflow.backend.repository.PaymentCheckoutRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class CheckoutService {

    private static final String PROVIDER = "cakto";

    private final PaymentCheckoutRequestRepository repository;

    @Value("${billing.checkout.base-url:https://seusite.com/checkout}")
    private String checkoutBaseUrl;

    public CheckoutService(PaymentCheckoutRequestRepository repository) {
        this.repository = repository;
    }

    public CheckoutResponse createCheckout(CheckoutRequest request) {

        String email = normalizeEmail(request.email());
        String referenceId = UUID.randomUUID().toString();

        PaymentCheckoutRequest checkoutRequest = new PaymentCheckoutRequest();
        checkoutRequest.setReferenceId(referenceId);
        checkoutRequest.setProvider(PROVIDER);
        checkoutRequest.setEmail(email);
        checkoutRequest.setNomeVendedor(request.nomeVendedor().trim());
        checkoutRequest.setWhatsappVendedor(request.whatsappVendedor().trim());
        checkoutRequest.setNomeEmpresa(normalizeNullable(request.nomeEmpresa()));
        checkoutRequest.setSlug(resolveSlug(request.slug(), request.nomeVendedor(), email));
        checkoutRequest.setStatus("PENDING");

        repository.save(checkoutRequest);

        String safeCheckoutBaseUrl =
                (checkoutBaseUrl == null || checkoutBaseUrl.isBlank())
                        ? "https://seusite.com/checkout"
                        : Objects.requireNonNull(checkoutBaseUrl);

        String checkoutUrl = UriComponentsBuilder
            .fromUriString(safeCheckoutBaseUrl)
                .queryParam("email", email)
                .queryParam("reference", referenceId)
                .queryParam("provider", PROVIDER)
                .build(true)
                .toUriString();

        return new CheckoutResponse(checkoutUrl, referenceId, PROVIDER);
    }

    public PaymentCheckoutRequest consumePendingByEmail(String email) {
        return repository
                .findTopByEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(normalizeEmail(email), "PENDING")
                .map(request -> {
                    request.setStatus("COMPLETED");
                    return repository.save(request);
                })
                .orElse(null);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String resolveSlug(String preferredSlug, String nomeVendedor, String email) {

        String base;
        if (preferredSlug != null && !preferredSlug.isBlank()) {
            base = slugify(preferredSlug);
        } else if (nomeVendedor != null && !nomeVendedor.isBlank()) {
            base = slugify(nomeVendedor);
        } else {
            String emailPrefix = email != null && email.contains("@") ? email.substring(0, email.indexOf('@')) : "vendor";
            base = slugify(emailPrefix);
        }

        if (base.isBlank()) {
            base = "vendor";
        }

        return base;
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        return normalized;
    }
}
