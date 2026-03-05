package com.leadflow.backend.controller;

import com.leadflow.backend.service.billing.TenantProvisioningService;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/billing/webhook")
public class StripeWebhookController {

    private final TenantProvisioningService tenantProvisioningService;

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    public StripeWebhookController(
            TenantProvisioningService tenantProvisioningService
    ) {
        this.tenantProvisioningService = tenantProvisioningService;
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {

        Event event;

        try {

            event = Webhook.constructEvent(
                    payload,
                    sigHeader,
                    endpointSecret
            );

        } catch (Exception e) {

            return ResponseEntity.status(400).build();
        }

        if ("checkout.session.completed".equals(event.getType())) {

            Session session = (Session) event.getDataObjectDeserializer()
                    .getObject()
                    .orElse(null);

            if (session != null) {

                tenantProvisioningService.provisionTenant(session);

            }

        }

        return ResponseEntity.ok().build();
    }
}