package com.leadflow.backend.service.lead;

import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.lead.LeadStatusHistory;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.lead.LeadRepository;
import com.leadflow.backend.repository.lead.LeadStatusHistoryRepository;
import com.leadflow.backend.repository.user.UserRepository;
import com.leadflow.backend.security.VendorContext;
import com.leadflow.backend.service.vendor.SubscriptionService;
import com.leadflow.backend.service.vendor.UsageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private LeadStatusHistoryRepository historyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VendorContext vendorContext;

    @Mock
    private UsageService usageService;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private LeadService leadService;

    private User user;
    private UUID leadId;

    @BeforeEach
    void setup() {

        Role role = new Role("ROLE_USER");
        ReflectionTestUtils.setField(role, "id", UUID.randomUUID());

        user = new User(
                "Test User",
                "test@example.com",
                "password",
                role
        );

        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        leadId = UUID.randomUUID();
    }

    /* ==========================
       CREATE
       ========================== */

    @Test
    @DisplayName("Should create lead and register history")
    void shouldCreateLead() {

        UUID tenantId = UUID.randomUUID();

        when(vendorContext.getCurrentVendorId()).thenReturn(tenantId);

        doNothing().when(subscriptionService).validateActiveSubscription(tenantId);

        when(leadRepository.existsByUserIdAndEmailIgnoreCaseAndDeletedAtIsNull(
                user.getId(),
                "lead@example.com"
        )).thenReturn(false);

        when(leadRepository.save(any(Lead.class)))
                .thenAnswer(invocation -> {
                    Lead lead = invocation.getArgument(0);
                    ReflectionTestUtils.setField(lead, "id", leadId);
                    return lead;
                });

        when(historyRepository.save(any(LeadStatusHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Lead created = leadService.createLead(
                "Lead",
                "lead@example.com",
                "123",
                user
        );

        assertThat(created.getId()).isEqualTo(leadId);
        assertThat(created.getStatus()).isEqualTo(LeadStatus.NEW);
        assertThat(created.getUserId()).isEqualTo(user.getId());

        verify(leadRepository).save(any(Lead.class));
        verify(historyRepository).save(any(LeadStatusHistory.class));
        verify(usageService).consumeLead(any(UUID.class));
    }

    @Test
    void shouldNotAllowDuplicateEmail() {

        when(leadRepository.existsByUserIdAndEmailIgnoreCaseAndDeletedAtIsNull(
                user.getId(),
                "duplicate@example.com"
        )).thenReturn(true);

        assertThatThrownBy(() ->
                leadService.createLead(
                        "New",
                        "duplicate@example.com",
                        "456",
                        user
                )
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Email already in use");
    }

    /* ==========================
       GET BY ID
       ========================== */

    @Test
    void shouldReturnLeadByIdForUser() {

        Lead lead = new Lead(
                user.getId(),
                "Lead",
                "lead@example.com",
                "123"
        );

        ReflectionTestUtils.setField(lead, "id", leadId);

        when(leadRepository
                .findByIdAndUserIdAndDeletedAtIsNull(leadId, user.getId()))
                .thenReturn(Optional.of(lead));

        Lead result =
                leadService.getByIdForUser(leadId, user.getId());

        assertThat(result).isEqualTo(lead);
    }

    @Test
    void shouldThrowIfLeadNotFound() {

        when(leadRepository
                .findByIdAndUserIdAndDeletedAtIsNull(leadId, user.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                leadService.getByIdForUser(leadId, user.getId())
        )
        .isInstanceOf(IllegalArgumentException.class);
    }

    /* ==========================
       UPDATE STATUS
       ========================== */

    @Test
    void shouldFollowValidLifecycle() {

        Lead lead = new Lead(
                user.getId(),
                "Lead",
                "lead@example.com",
                "123"
        );

        ReflectionTestUtils.setField(lead, "id", leadId);

        when(leadRepository
                .findByIdAndUserIdAndDeletedAtIsNull(leadId, user.getId()))
                .thenReturn(Optional.of(lead));

        leadService.updateStatus(leadId, LeadStatus.CONTACTED, user);
        leadService.updateStatus(leadId, LeadStatus.QUALIFIED, user);
        leadService.updateStatus(leadId, LeadStatus.CLOSED, user);

        assertThat(lead.getStatus())
                .isEqualTo(LeadStatus.CLOSED);

        verify(historyRepository, times(3))
                .save(any(LeadStatusHistory.class));
    }

    @Test
    void shouldBlockInvalidTransition() {

        Lead lead = new Lead(
                user.getId(),
                "Lead",
                "lead@example.com",
                "123"
        );

        ReflectionTestUtils.setField(lead, "id", leadId);

        lead.changeStatus(LeadStatus.CONTACTED);

        when(leadRepository
                .findByIdAndUserIdAndDeletedAtIsNull(leadId, user.getId()))
                .thenReturn(Optional.of(lead));

        assertThatThrownBy(() ->
                leadService.updateStatus(
                        leadId,
                        LeadStatus.NEW,
                        user
                )
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid status transition");
    }

    /* ==========================
       SOFT DELETE
       ========================== */

    @Test
    void shouldSoftDeleteLead() {

        Lead lead = new Lead(
                user.getId(),
                "Lead",
                "lead@example.com",
                "123"
        );

        ReflectionTestUtils.setField(lead, "id", leadId);

        when(leadRepository
                .findByIdAndUserIdAndDeletedAtIsNull(leadId, user.getId()))
                .thenReturn(Optional.of(lead));

        leadService.softDelete(leadId, user);

        assertThat(lead.getDeletedAt()).isNotNull();

        verify(leadRepository, never()).save(any());
    }
}