package com.leadflow.backend.controller;

import com.leadflow.backend.dto.ai.ChatRequest;
import com.leadflow.backend.entities.vendor.VendorLeadConversation;
import com.leadflow.backend.service.ai.AiService;
import com.leadflow.backend.service.vendor.ConversationService;
import com.leadflow.backend.service.vendor.VendorLeadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiService aiService;
    private final ConversationService conversationService;
    private final VendorLeadService vendorLeadService;

    public AiController(AiService aiService,
                        ConversationService conversationService,
                        VendorLeadService vendorLeadService) {
        this.aiService = aiService;
        this.conversationService = conversationService;
        this.vendorLeadService = vendorLeadService;
    }

    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody ChatRequest request) {

        if (request.getLeadId() == null ||
            request.getMessage() == null ||
            request.getMessage().isBlank()) {

            return ResponseEntity.badRequest().body("Dados inválidos.");
        }

        UUID leadId = request.getLeadId();

        vendorLeadService.getLeadForCurrentVendor(leadId);

        conversationService.saveMessage(leadId, "USER", request.getMessage());

        List<VendorLeadConversation> history =
                conversationService.getConversation(leadId);

        StringBuilder contextBuilder = new StringBuilder();

        for (VendorLeadConversation msg : history) {
            contextBuilder
                    .append(msg.getRole())
                    .append(": ")
                    .append(msg.getContent())
                    .append("\n");
        }

        String fullContext = contextBuilder.toString();

        String aiResponse = aiService.generate(fullContext);

        conversationService.saveMessage(leadId, "AI", aiResponse);

        return ResponseEntity.ok(aiResponse);
    }
}