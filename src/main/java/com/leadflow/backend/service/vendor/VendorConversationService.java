package com.leadflow.backend.service.vendor;

import com.leadflow.backend.entities.vendor.VendorLeadConversation;
import com.leadflow.backend.repository.vendor.VendorLeadConversationRepository;
import com.leadflow.backend.multitenancy.context.TenantContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class VendorConversationService {

    private final VendorLeadConversationRepository repository;

    public VendorConversationService(VendorLeadConversationRepository repository) {
        this.repository = repository;
    }

    /**
     * Processa uma mensagem enviada para a conversa com o lead
     */
    @Transactional
    public VendorLeadConversation processMessage(
            UUID vendorLeadId,
            UUID leadId,
            String message,
            String sender
    ) {

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }

        VendorLeadConversation conversation = new VendorLeadConversation();

        conversation.setVendorLeadId(vendorLeadId);
        conversation.setLeadId(leadId);
        conversation.setContent(message);
        conversation.setSender(sender);
        conversation.setTenant(TenantContext.getTenant());
        conversation.setCreatedAt(Instant.now());

        return repository.save(conversation);
    }
}