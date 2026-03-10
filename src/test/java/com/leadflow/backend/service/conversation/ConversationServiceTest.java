package com.leadflow.backend.service.conversation;

import com.leadflow.backend.dto.ai.ChatRequest;
import com.leadflow.backend.entities.vendor.VendorLeadConversation;
import com.leadflow.backend.entities.vendor.VendorLead;
import com.leadflow.backend.repository.VendorLeadRepository;
import com.leadflow.backend.repository.vendor.VendorLeadConversationRepository;
import com.leadflow.backend.service.ai.AiService;
import com.leadflow.backend.service.vendor.UsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationServiceTest {

    private VendorLeadRepository leadRepository;
    private VendorLeadConversationRepository conversationRepository;
    private AiService aiService;
    private UsageService usageService;
    private ConversationService service;

    @BeforeEach
    void setup() {
        leadRepository = mock(VendorLeadRepository.class);
        conversationRepository = mock(VendorLeadConversationRepository.class);
        aiService = mock(AiService.class);
        usageService = mock(UsageService.class);
        service = new ConversationService(leadRepository, conversationRepository, aiService, usageService);
    }

    @Test
    void shouldFollowConversationFlowAndPersistMessages() {
        UUID leadId = UUID.randomUUID();

        ChatRequest request = new ChatRequest();
        request.setLeadId(leadId);
        request.setMessage("Olá, quero saber sobre consórcio");

        when(leadRepository.existsById(any(UUID.class))).thenReturn(true);
        VendorLead lead = new VendorLead();
        lead.setVendorId(UUID.randomUUID());
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(conversationRepository.findByVendorLeadIdOrderByCreatedAtAsc(leadId))
                .thenReturn(List.of());
        when(aiService.generate(anyString())).thenReturn("Resposta da IA");

        String response = service.chat(request);

        assertEquals("Resposta da IA", response);

        var order = inOrder(conversationRepository, aiService, conversationRepository);
        order.verify(conversationRepository).save(any(VendorLeadConversation.class));
        order.verify(conversationRepository).findByVendorLeadIdOrderByCreatedAtAsc(leadId);
        order.verify(aiService).generate(anyString());
        order.verify(conversationRepository).save(any(VendorLeadConversation.class));
    }

    @Test
    void shouldThrowWhenLeadNotFound() {
        UUID leadId = UUID.randomUUID();

        ChatRequest request = new ChatRequest();
        request.setLeadId(leadId);
        request.setMessage("oi");

        when(leadRepository.existsById(leadId)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.chat(request));

        assertEquals("Lead não encontrado.", ex.getMessage());
        verify(conversationRepository, never()).save(any(VendorLeadConversation.class));
        verify(aiService, never()).generate(anyString());
    }

    @Test
    void shouldPersistUserAndAiRoles() {
        UUID leadId = UUID.randomUUID();

        ChatRequest request = new ChatRequest();
        request.setLeadId(leadId);
        request.setMessage("mensagem do usuário");

        when(leadRepository.existsById(leadId)).thenReturn(true);
        VendorLead lead = new VendorLead();
        lead.setVendorId(UUID.randomUUID());
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(conversationRepository.findByVendorLeadIdOrderByCreatedAtAsc(leadId))
                .thenReturn(List.of());
        when(aiService.generate(anyString())).thenReturn("mensagem da ia");

        service.chat(request);

        var captor = org.mockito.ArgumentCaptor.forClass(VendorLeadConversation.class);
        verify(conversationRepository, org.mockito.Mockito.times(2)).save(captor.capture());

        List<VendorLeadConversation> messages = captor.getAllValues();
        assertEquals("USER", messages.get(0).getRole());
        assertEquals("mensagem do usuário", messages.get(0).getContent());
        assertEquals("AI", messages.get(1).getRole());
        assertEquals("mensagem da ia", messages.get(1).getContent());
    }
}