package com.leadflow.backend.service.conversation;

import com.leadflow.backend.dto.ai.ChatRequest;
import com.leadflow.backend.entities.vendor.VendorLeadConversation;
import com.leadflow.backend.repository.VendorLeadRepository;
import com.leadflow.backend.repository.vendor.VendorLeadConversationRepository;
import com.leadflow.backend.service.ai.AiService;
import com.leadflow.backend.service.vendor.UsageService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service("chatConversationService")
public class ConversationService {

    private static final int HISTORY_LIMIT = 10;
    private static final String USER_ROLE = "USER";
    private static final String AI_ROLE = "AI";

    private final VendorLeadRepository leadRepository;
    private final VendorLeadConversationRepository conversationRepository;
    private final AiService aiService;
    private final UsageService usageService;

    public ConversationService(VendorLeadRepository leadRepository,
                               VendorLeadConversationRepository conversationRepository,
                               AiService aiService,
                               UsageService usageService) {
        this.leadRepository = leadRepository;
        this.conversationRepository = conversationRepository;
        this.aiService = aiService;
        this.usageService = usageService;
    }

    public String chat(ChatRequest request) {
        if (request == null || request.getLeadId() == null) {
            throw new IllegalArgumentException("leadId é obrigatório.");
        }

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("Mensagem inválida.");
        }

        UUID leadId = Objects.requireNonNull(request.getLeadId());
        String userMessage = request.getMessage().trim();

        if (!leadRepository.existsById(leadId)) {
            throw new IllegalArgumentException("Lead não encontrado.");
        }

        saveConversation(leadId, USER_ROLE, userMessage);

        List<VendorLeadConversation> history =
            conversationRepository.findByVendorLeadIdOrderByCreatedAtAsc(leadId);

        UUID tenantId = leadRepository.findById(leadId)
            .orElseThrow(() -> new IllegalArgumentException("Lead não encontrado."))
            .getVendorId();

        usageService.consumeAiExecution(tenantId);

        String prompt = buildPromptWithContext(history);
        String aiResponse = aiService.generate(prompt);

        saveConversation(leadId, AI_ROLE, aiResponse);

        return aiResponse;
    }

    private String buildPromptWithContext(List<VendorLeadConversation> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }

        int start = Math.max(0, history.size() - HISTORY_LIMIT);

        StringBuilder prompt = new StringBuilder();
        prompt.append("Histórico de conversa do lead (ordem cronológica):\n");

        for (int i = start; i < history.size(); i++) {
            VendorLeadConversation item = history.get(i);
            prompt.append(item.getRole())
                    .append(": ")
                    .append(item.getContent())
                    .append("\n");
        }

        return prompt.toString();
    }

    private void saveConversation(UUID leadId, String role, String content) {
        UUID safeLeadId = Objects.requireNonNull(leadId);
        VendorLeadConversation conversation = new VendorLeadConversation();
        conversation.setVendorLeadId(safeLeadId);
        conversation.setRole(role);
        conversation.setContent(content.trim());
        conversationRepository.save(conversation);
    }
}