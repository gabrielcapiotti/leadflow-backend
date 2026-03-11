package com.leadflow.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadflow.backend.dto.ai.ChatRequest;
import com.leadflow.backend.entities.vendor.SubscriptionAccessLevel;
import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.entities.vendor.VendorFeatureKey;
import com.leadflow.backend.exception.GlobalExceptionHandler;
import com.leadflow.backend.multitenancy.service.TenantService;
import com.leadflow.backend.security.RateLimitService;
import com.leadflow.backend.security.SubscriptionGuard;
import com.leadflow.backend.security.VendorContext;
import com.leadflow.backend.service.ai.AiRateLimiter;
import com.leadflow.backend.service.ai.AiService;
import com.leadflow.backend.service.monitoring.AiMetricsService;
import com.leadflow.backend.service.vendor.ConversationService;
import com.leadflow.backend.service.vendor.QuotaService;
import com.leadflow.backend.service.vendor.VendorFeatureService;
import com.leadflow.backend.service.vendor.VendorLeadService;

import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;

import org.springframework.http.MediaType;

import org.springframework.security.test.context.support.WithMockUser;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AiController.class
)
@ContextConfiguration(classes = AiController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /* ================= MOCKS ================= */

    @MockitoBean
    private AiService aiService;

    @MockitoBean
    private ConversationService conversationService;

    @MockitoBean
    private VendorLeadService vendorLeadService;

    @MockitoBean
    private SubscriptionGuard subscriptionGuard;

    @MockitoBean
    private VendorContext vendorContext;

    @MockitoBean
    private QuotaService quotaService;

    @MockitoBean
    private AiRateLimiter aiRateLimiter;

    @MockitoBean
    private AiMetricsService aiMetricsService;

    @MockitoBean
    private VendorFeatureService vendorFeatureService;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private RateLimitService rateLimitService;

    /* FIX para erro de entityManagerFactory */
    @MockitoBean
    private EntityManagerFactory entityManagerFactory;

    private UUID vendorId;

    /* ================= SETUP ================= */

    @BeforeEach
    void setUp() {

        vendorId = UUID.randomUUID();

        Vendor vendor = new Vendor();
        ReflectionTestUtils.setField(vendor, "id", vendorId);
        vendor.setUserEmail("test@example.com");

        when(vendorContext.getCurrentVendor()).thenReturn(vendor);

        when(subscriptionGuard.resolveAccess())
                .thenReturn(SubscriptionAccessLevel.FULL);

        when(subscriptionGuard.isActive())
                .thenReturn(true);
    }

    /* ================= TESTS ================= */

    @Test
    @WithMockUser(username = "test@example.com")
    @SuppressWarnings("null")
    void chatShouldReturnForbiddenWhenFeatureDisabled() throws Exception {

        ChatRequest request = new ChatRequest();
        request.setLeadId(UUID.randomUUID());
        request.setMessage("olá");

        when(vendorFeatureService.isEnabled(vendorId, VendorFeatureKey.AI_CHAT))
                .thenReturn(false);

        when(aiRateLimiter.allow(vendorId)).thenReturn(true);

        mockMvc.perform(
                        post("/ai/chat")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FEATURE_DISABLED"));

        verify(aiService, never()).generate(any());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @SuppressWarnings("null")
    void chatShouldSucceedWhenFeatureEnabled() throws Exception {

        ChatRequest request = new ChatRequest();
        request.setLeadId(UUID.randomUUID());
        request.setMessage("responda");

        when(vendorFeatureService.isEnabled(vendorId, VendorFeatureKey.AI_CHAT))
                .thenReturn(true);

        when(aiRateLimiter.allow(vendorId)).thenReturn(true);

        when(conversationService.getConversation(any()))
                .thenReturn(Collections.emptyList());

        when(aiService.generate(any()))
                .thenReturn("ok");

        mockMvc.perform(
                        post("/ai/chat")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }
}