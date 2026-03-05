package com.leadflow.backend.dto.billing;

public record CheckoutResponse(
        String checkoutUrl,
        String referenceId,
        String provider
) {
}
