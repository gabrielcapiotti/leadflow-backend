package com.leadflow.backend.controller;

import com.leadflow.backend.service.billing.BillingTenantProvisioningService;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/billing/webhook")
public class StripeWebhookController {

    private final BillingTenantProvisioningService provisioningService;

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    public StripeWebhookController(
            BillingTenantProvisioningService provisioningService
    ) {
        this.provisioningService = provisioningService;
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature
    ) {

        Event event;

        try {

            event = Webhook.constructEvent(
                    payload,
                    signature,
                    endpointSecret
            );

        } catch (Exception e) {

            return ResponseEntity.badRequest().build();
        }

        if ("checkout.session.completed".equals(event.getType())) {

            Session session = (Session) event
                    .getDataObjectDeserializer()
                    .getObject()
                    .orElse(null);

            if (session != null) {

                provisioningService.provisionFromCheckout(session);

            }

        }

        return ResponseEntity.ok().build();
    }
}