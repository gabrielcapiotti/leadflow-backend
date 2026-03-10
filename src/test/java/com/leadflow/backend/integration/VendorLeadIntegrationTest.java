package com.leadflow.backend.integration;

import com.leadflow.backend.entities.vendor.*;
import com.leadflow.backend.config.TestBillingConfig;
import com.leadflow.backend.repository.*;
import com.leadflow.backend.security.VendorContext;
import com.leadflow.backend.service.admin.AdminService;
import com.leadflow.backend.service.ai.AiService;
import com.leadflow.backend.service.audit.AuditService;
import com.leadflow.backend.service.monitoring.MetricsService;
import com.leadflow.backend.service.notification.SendGridEmailService;
import com.leadflow.backend.service.vendor.VendorLeadService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestBillingConfig.class)
@ActiveProfiles("test")
@Transactional
class VendorLeadIntegrationTest {

    @MockBean
    private VendorContext vendorContext;

    @MockBean
    private MetricsService metricsService;

    @MockBean
    private SendGridEmailService sendGridEmailService;

    @MockBean
    private AiService aiService;

    @MockBean
    private AuditService auditService;

    @MockBean
    private AdminService adminService;

    @Autowired
    private VendorLeadRepository leadRepository;

    @Autowired
    private VendorLeadStageHistoryRepository historyRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private VendorLeadService service;

    private Vendor vendor;

    @BeforeEach
    void setup() {

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "vendor@test.com",
                        "N/A"
                )
        );

        // Criar vendor
        vendor = new Vendor();
        vendor.setUserEmail("vendor@test.com");
        vendor.setName("Vendor Teste");
        vendor.setNomeVendedor("Teste");
        vendor.setWhatsappVendedor("99999999");
        vendor.setSlug("teste");
        vendor.setUpdatedAt(Instant.now());

        vendorRepository.save(vendor);

        // Mock do VendorContext
        Mockito.when(vendorContext.getCurrentVendor())
                .thenReturn(vendor);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPersistLeadAndStageHistory() {

        // Criar lead
        VendorLead lead = new VendorLead();
        lead.setVendorId(vendor.getId());
        lead.setNomeCompleto("Cliente");
        lead.setWhatsapp("88888888");
        lead.setStage(LeadStage.NOVO);

        leadRepository.save(lead);

        java.util.UUID safeLeadId = java.util.Objects.requireNonNull(lead.getId());

        // Atualizar estágio
        service.updateStage(safeLeadId, LeadStage.CONTATO);

        // Verificar no banco
        VendorLead updated =
                leadRepository.findById(safeLeadId).orElseThrow();

        assertEquals(LeadStage.CONTATO, updated.getStage());

        // Verificar histórico
        var history =
                historyRepository
                        .findByVendorLeadIdOrderByChangedAtDesc(safeLeadId);

        assertFalse(history.isEmpty());
        assertEquals("NOVO", history.get(0).getPreviousStage());
        assertEquals("CONTATO", history.get(0).getNewStage());
    }
}