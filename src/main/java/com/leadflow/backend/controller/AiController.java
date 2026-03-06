package com.leadflow.backend.controller;

import com.leadflow.backend.dto.ai.ChatRequest;
import com.leadflow.backend.entities.vendor.SubscriptionAccessLevel;
import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.entities.vendor.VendorFeatureKey;
import com.leadflow.backend.entities.vendor.VendorLeadConversation;
import com.leadflow.backend.security.SubscriptionGuard;
import com.leadflow.backend.security.VendorContext;
import com.leadflow.backend.service.ai.AiRateLimiter;
import com.leadflow.backend.service.ai.AiService;
import com.leadflow.backend.service.monitoring.AiMetricsService;
import com.leadflow.backend.service.vendor.ConversationService;
import com.leadflow.backend.service.vendor.VendorFeatureService;
import com.leadflow.backend.service.vendor.VendorLeadService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ai")
@PreAuthorize("@subscriptionGuard.isActive()")
public class AiController {

    private final AiService aiService;
    private final ConversationService conversationService;
    private final VendorLeadService vendorLeadService;
    private final SubscriptionGuard subscriptionGuard;
    private final VendorContext vendorContext;
    private final AiRateLimiter aiRateLimiter;
    private final AiMetricsService aiMetricsService;
    private final VendorFeatureService vendorFeatureService;

    public AiController(
            AiService aiService,
            ConversationService conversationService,
            VendorLeadService vendorLeadService,
            SubscriptionGuard subscriptionGuard,
            VendorContext vendorContext,
            AiRateLimiter aiRateLimiter,
            AiMetricsService aiMetricsService,
            VendorFeatureService vendorFeatureService
    ) {
        this.aiService = aiService;
        this.conversationService = conversationService;
        this.vendorLeadService = vendorLeadService;
        this.subscriptionGuard = subscriptionGuard;
        this.vendorContext = vendorContext;
        this.aiRateLimiter = aiRateLimiter;
        this.aiMetricsService = aiMetricsService;
        this.vendorFeatureService = vendorFeatureService;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@Valid @RequestBody ChatRequest request) {

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "error", "INVALID_MESSAGE",
                            "message", "Mensagem não pode estar vazia"
                    )
            );
        }

        if (subscriptionGuard.resolveAccess() != SubscriptionAccessLevel.FULL) {
            return ResponseEntity.status(403).body(
                    Map.of(
                            "error", "SUBSCRIPTION_READ_ONLY",
                            "message", "Assinatura não permite uso da IA."
                    )
            );
        }

        Vendor vendor = vendorContext.getCurrentVendor();
        if (vendor == null) {
            return ResponseEntity.status(401).body(
                    Map.of(
                            "error", "UNAUTHORIZED",
                            "message", "Vendor não autenticado"
                    )
            );
        }

        UUID vendorId = vendor.getId();

        if (!vendorFeatureService.isEnabled(vendorId, VendorFeatureKey.AI_CHAT)) {
            return ResponseEntity.status(403).body(
                    Map.of(
                            "error", "FEATURE_DISABLED",
                            "message", "Recurso de IA não habilitado para esta conta."
                    )
            );
        }

        if (!aiRateLimiter.allow(vendorId)) {
            return ResponseEntity.status(429).body(
                    Map.of(
                            "error", "RATE_LIMIT",
                            "message", "Limite de uso temporário atingido"
                    )
            );
        }

        UUID leadId = request.getLeadId();

        // valida acesso ao lead
        vendorLeadService.getLeadForCurrentVendor(leadId);

        // salva mensagem do usuário
        conversationService.saveMessage(leadId, "USER", request.getMessage());

        List<VendorLeadConversation> history =
                conversationService.getConversation(leadId);

        String fullContext = history == null
                ? ""
                : history.stream()
                .map(m -> m.getRole() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

        aiMetricsService.increment();

        String aiResponse = aiService.generate(fullContext);

        conversationService.saveMessage(leadId, "AI", aiResponse);

        return ResponseEntity.ok(aiResponse);
    }
}