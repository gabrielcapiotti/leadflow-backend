package com.leadflow.backend.controller;

import com.leadflow.backend.dto.CheckoutRequest;
import com.leadflow.backend.service.billing.StripeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/billing")
public class BillingController {

    private final StripeService stripeService;

    public BillingController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @PostMapping("/checkout-session")
    public ResponseEntity<Map<String, String>> createCheckoutSession(
            @RequestBody CheckoutRequest request
    ) {

        String url = stripeService.createCheckoutSession(request);

        return ResponseEntity.ok(Map.of("url", url));
    }
}
