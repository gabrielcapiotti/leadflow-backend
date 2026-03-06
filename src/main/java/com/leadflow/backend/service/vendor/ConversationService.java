package com.leadflow.backend.service.vendor;

import com.leadflow.backend.entities.vendor.VendorLeadConversation;
import com.leadflow.backend.repository.vendor.VendorLeadConversationRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ConversationService {

    private final VendorLeadConversationRepository repository;

    public ConversationService(VendorLeadConversationRepository repository) {
        this.repository = repository;
    }

    public void saveMessage(UUID leadId, String role, String content) {

        VendorLeadConversation message = new VendorLeadConversation();
        message.setVendorLeadId(leadId);
        message.setRole(role);
        message.setContent(content);

        repository.save(message);
    }

    public List<VendorLeadConversation> getConversation(UUID leadId) {
        return repository.findByVendorLeadIdOrderByCreatedAtAsc(leadId);
    }
}