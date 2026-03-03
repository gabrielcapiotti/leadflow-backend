package com.leadflow.backend.integration;

import com.leadflow.backend.entities.vendor.*;
import com.leadflow.backend.repository.*;
import com.leadflow.backend.service.vendor.VendorLeadService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VendorLeadIntegrationTest {

    @Autowired
    private VendorLeadRepository leadRepository;

    @Autowired
    private VendorLeadStageHistoryRepository historyRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private VendorLeadService service;

    @BeforeEach
    void setAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("vendor@test.com", "N/A")
        );
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPersistLeadAndStageHistory() {

        // Criar vendor real
        Vendor vendor = new Vendor();
        vendor.setUserEmail("vendor@test.com");
        vendor.setNomeVendedor("Teste");
        vendor.setWhatsappVendedor("99999999");
        vendor.setSlug("teste");

        vendorRepository.save(vendor);

        // Criar lead
        VendorLead lead = new VendorLead();
        lead.setVendorId(vendor.getId());
        lead.setNomeCompleto("Cliente");
        lead.setWhatsapp("88888888");

        leadRepository.save(lead);

        // Atualizar estágio
        service.updateStage(lead.getId(), LeadStage.CONTATO);

        // Verificar no banco
        VendorLead updated =
                leadRepository.findById(lead.getId()).orElseThrow();

        assertEquals(LeadStage.CONTATO, updated.getStage());

        // Verificar histórico
        var history = historyRepository
                .findByVendorLeadIdOrderByChangedAtDesc(lead.getId());

        assertFalse(history.isEmpty());
        assertEquals("NOVO", history.get(0).getPreviousStage());
        assertEquals("CONTATO", history.get(0).getNewStage());
    }
}