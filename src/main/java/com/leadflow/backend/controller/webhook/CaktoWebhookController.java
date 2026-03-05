package com.leadflow.backend.controller.webhook;

import com.leadflow.backend.dto.billing.CaktoWebhookPayload;
import com.leadflow.backend.service.billing.CaktoWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/cakto")
public class CaktoWebhookController {

    private final CaktoWebhookService service;

    public CaktoWebhookController(CaktoWebhookService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Void> handle(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret,
            @RequestBody CaktoWebhookPayload payload
    ) {
        try {
            service.process(payload, secret);
            return ResponseEntity.ok().build();
        } catch (SecurityException ignored) {
            return ResponseEntity.status(401).build();
        }
    }
}
