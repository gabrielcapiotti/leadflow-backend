package com.leadflow.backend.service.lead;

import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.lead.LeadStatusHistory;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.lead.LeadRepository;
import com.leadflow.backend.repository.lead.LeadStatusHistoryRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private LeadStatusHistoryRepository historyRepository;

    @InjectMocks
    private LeadService leadService;

    private User user;
    private UUID leadId;

    @BeforeEach
    void setup() {

        Role role = new Role("USER");
        ReflectionTestUtils.setField(role, "id", UUID.randomUUID());

        Tenant tenant = new Tenant("Test Tenant", "test_schema");

        user = new User(
                "Test User",
                "test@example.com",
                "password",
                role,
                tenant
        );

        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        leadId = UUID.randomUUID();

        lenient().when(leadRepository.save(any(Lead.class)))
                .thenAnswer(invocation -> {
                    Lead lead = invocation.getArgument(0);
                    ReflectionTestUtils.setField(lead, "id", leadId);
                    return lead;
                });
    }

    /* ==========================
       CREATE
       ========================== */

    @Test
    @DisplayName("Should create lead and register history")
    void shouldCreateLead() {

        when(leadRepository.findByUserIdAndDeletedAtIsNull(user.getId()))
                .thenReturn(List.of());

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
    }

    @Test
    void shouldThrowWhenUserIsNull() {
        assertThatThrownBy(() ->
                leadService.createLead("Lead", "lead@example.com", "123", null)
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("User cannot be null");
    }

    @Test
    void shouldNotAllowDuplicateEmail() {

        Lead existing = new Lead(
                user.getId(),
                "Existing",
                "duplicate@example.com",
                "123"
        );

        when(leadRepository.findByUserIdAndDeletedAtIsNull(user.getId()))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() ->
                leadService.createLead("New", "duplicate@example.com", "456", user)
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Email already in use");
    }

    /* ==========================
       LIST
       ========================== */

    @Test
    void shouldListActiveLeads() {

        Lead lead = new Lead(
                user.getId(),
                "Lead",
                "lead@example.com",
                "123"
        );

        when(leadRepository.findByUserIdAndDeletedAtIsNull(user.getId()))
                .thenReturn(List.of(lead));

        List<Lead> result = leadService.listActiveLeads(user);

        assertThat(result).hasSize(1);
        verify(leadRepository)
                .findByUserIdAndDeletedAtIsNull(user.getId());
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

        lead.changeStatus(LeadStatus.CONTACTED);

        when(leadRepository
                .findByIdAndUserIdAndDeletedAtIsNull(leadId, user.getId()))
                .thenReturn(Optional.of(lead));

        assertThatThrownBy(() ->
                leadService.updateStatus(leadId, LeadStatus.NEW, user)
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid status transition");
    }

    @Test
    void shouldNotSaveHistoryIfStatusIsSame() {

        Lead lead = new Lead(
                user.getId(),
                "Lead",
                "lead@example.com",
                "123"
        );

        when(leadRepository
                .findByIdAndUserIdAndDeletedAtIsNull(leadId, user.getId()))
                .thenReturn(Optional.of(lead));

        leadService.updateStatus(leadId, LeadStatus.NEW, user);

        verify(historyRepository, never()).save(any());
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

        when(leadRepository
                .findByIdAndUserIdAndDeletedAtIsNull(leadId, user.getId()))
                .thenReturn(Optional.of(lead));

        leadService.softDelete(leadId, user);

        assertThat(lead.getDeletedAt()).isNotNull();
        verify(leadRepository).save(lead);
    }

    @Test
    void shouldThrowWhenDeletingLeadNotFound() {

        when(leadRepository
                .findByIdAndUserIdAndDeletedAtIsNull(leadId, user.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                leadService.softDelete(leadId, user)
        )
        .isInstanceOf(IllegalArgumentException.class);
    }
}