package com.leadflow.backend.service.vendor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadflow.backend.dto.vendor.StageConversionResponse;
import com.leadflow.backend.dto.vendor.StageTimeMetricsResponse;
import com.leadflow.backend.entities.vendor.*;
import com.leadflow.backend.repository.*;
import com.leadflow.backend.repository.vendor.VendorLeadConversationRepository;
import com.leadflow.backend.security.VendorContext;
import com.leadflow.backend.service.monitoring.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VendorLeadServiceTest {

    private VendorLeadRepository repository;
    private VendorLeadConversationRepository conversationRepository;
    private VendorLeadStageHistoryRepository historyRepository;
    private VendorContext vendorContext;
    private MetricsService metricsService;

    private VendorLeadService service;

    private UUID vendorId;

    @BeforeEach
    void setup() {

        repository = mock(VendorLeadRepository.class);
        conversationRepository = mock(VendorLeadConversationRepository.class);
        historyRepository = mock(VendorLeadStageHistoryRepository.class);
        vendorContext = mock(VendorContext.class);
        metricsService = mock(MetricsService.class);

        service = new VendorLeadService(
                repository,
                conversationRepository,
                historyRepository,
                vendorContext,
                metricsService,
                new ObjectMapper()
        );

        vendorId = UUID.randomUUID();

        Vendor vendor = new Vendor();
        vendor.setId(vendorId);
        vendor.setUserEmail("user@test.com");

        when(vendorContext.getCurrentVendor()).thenReturn(vendor);

        when(repository.save(any(VendorLead.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        when(conversationRepository.save(any(VendorLeadConversation.class))).thenReturn(new VendorLeadConversation());
    }

    // ========================================
    // STAGE UPDATE
    // ========================================

    @Test
    void shouldUpdateStageSuccessfully() {

        UUID leadId = UUID.randomUUID();

        VendorLead lead = new VendorLead();
        lead.setVendorId(vendorId);
        lead.setStage(LeadStage.NOVO);

        setId(lead, leadId);

        when(repository.findByIdAndVendorId(leadId, vendorId))
                .thenReturn(Optional.of(lead));

        VendorLead updated = service.updateStage(leadId, LeadStage.CONTATO);

        assertEquals(LeadStage.CONTATO, updated.getStage());
        verify(historyRepository).save(any(VendorLeadStageHistory.class));
    }

    @Test
    void shouldThrowWhenInvalidTransition() {

        UUID leadId = UUID.randomUUID();

        VendorLead lead = new VendorLead();
        lead.setVendorId(vendorId);
        lead.setStage(LeadStage.CONTATO);

        setId(lead, leadId);

        when(repository.findByIdAndVendorId(leadId, vendorId))
                .thenReturn(Optional.of(lead));

        assertThrows(IllegalStateException.class,
                () -> service.updateStage(leadId, LeadStage.NOVO));
    }

    // ========================================
    // OWNER
    // ========================================

    @Test
    void shouldAssignOwner() {

        UUID leadId = UUID.randomUUID();

        VendorLead lead = new VendorLead();
        lead.setVendorId(vendorId);

        setId(lead, leadId);

        when(repository.findByIdAndVendorId(leadId, vendorId))
                .thenReturn(Optional.of(lead));

        VendorLead result = service.assignOwner(leadId);

        assertEquals("user@test.com", result.getOwnerEmail());
    }

    // ========================================
    // CONVERSION
    // ========================================

    @Test
    void shouldCalculateConversionRates() {

        List<Object[]> data = Collections.singletonList(
                new Object[]{"NOVO", "CONTATO", 2L}
        );

        when(historyRepository.countTransitionsByVendor(vendorId))
            .thenReturn(data);

        StageConversionResponse response =
                service.calculateConversionRatesForCurrentVendor();

        assertEquals(100.0,
                response.getConversionRates().get("NOVO→CONTATO"));
    }

    // ========================================
    // STAGE TIME
    // ========================================

    @Test
    void shouldCalculateAverageStageTime() {

        UUID leadId = UUID.randomUUID();

        Instant created = Instant.now().minusSeconds(4 * 3600);
        Instant change = created.plusSeconds(2 * 3600);

        VendorLead lead = new VendorLead();
        lead.setVendorId(vendorId);
        lead.setStage(LeadStage.CONTATO);
        setId(lead, leadId);

        setCreatedDate(lead, created);

        VendorLeadStageHistory history = new VendorLeadStageHistory();
        history.setVendorLeadId(leadId);
        history.setPreviousStage("NOVO");
        history.setNewStage("CONTATO");
        setChangedAt(history, change);

        when(repository.findByVendorId(vendorId))
                .thenReturn(List.of(lead));

        when(historyRepository.findByVendorLeadIdOrderByChangedAtDesc(leadId))
                .thenReturn(List.of(history));

        StageTimeMetricsResponse response =
                service.calculateAverageStageTimeForCurrentVendor();

        assertEquals(2.0,
                response.getAverageTimeInHours().get("NOVO"));
    }

    // ========================================
    // CONVERSATION
    // ========================================

    @Test
    void shouldSaveConversation() {

        UUID leadId = UUID.randomUUID();

        service.saveConversation(leadId, "Olá", "Resposta");

        verify(conversationRepository, times(2))
            .save(any(VendorLeadConversation.class));
    }

    // ========================================
    // Helpers
    // ========================================

    private void setId(VendorLead lead, UUID id) {
        try {
            var field = VendorLead.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(lead, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setCreatedDate(VendorLead lead, Instant instant) {
        try {
            var field = VendorLead.class.getDeclaredField("createdDate");
            field.setAccessible(true);
            field.set(lead, instant);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setChangedAt(VendorLeadStageHistory history, Instant instant) {
        try {
            var field = VendorLeadStageHistory.class.getDeclaredField("changedAt");
            field.setAccessible(true);
            field.set(history, instant);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}