package com.leadflow.backend.service.lead;

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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private LeadStatusHistoryRepository historyRepository;

    @InjectMocks
    private LeadService leadService;

    private User user;

    @BeforeEach
    void setup() {
        Role role = new Role("USER");
        ReflectionTestUtils.setField(role, "id", 1);

        user = new User("Test User", "test@example.com", "password", role);
        ReflectionTestUtils.setField(user, "id", 1L);

        lenient().when(leadRepository.save(any(Lead.class)))
                .thenAnswer(invocation -> {
                    Lead lead = invocation.getArgument(0);
                    ReflectionTestUtils.setField(lead, "id", 1L);
                    return lead;
                });
    }

    /* ==========================
       CREATE
       ========================== */

    @Test
    @DisplayName("Should create lead and register correct history")
    void shouldCreateLead() {

        Lead created = leadService.createLead(
                "Lead",
                "lead@example.com",
                "123",
                user
        );

        assertThat(created.getId()).isEqualTo(1L);
        assertThat(created.getUser()).isEqualTo(user);
        assertThat(created.getStatus()).isEqualTo(LeadStatus.NEW);

        verify(leadRepository).save(any(Lead.class));

        verify(historyRepository).save(argThat(history ->
                history.getLead().equals(created) &&
                history.getStatus() == LeadStatus.NEW &&
                history.getChangedBy().equals(user)
        ));
    }

    @Test
    void shouldThrowWhenUserIsNull() {
        assertThatThrownBy(() ->
                leadService.createLead("Lead", "lead@example.com", "123", null)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessage("User cannot be null");
    }

    @Test
    void shouldNotAllowDuplicateEmail() {

        Lead existing = new Lead("Existing", "duplicate@example.com", "123");
        existing.setUser(user);

        when(leadRepository.findByUserAndDeletedAtIsNull(user))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() ->
                leadService.createLead("New", "duplicate@example.com", "456", user)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessage("Email already in use");
    }

    /* ==========================
       LIST
       ========================== */

    @Test
    void shouldListActiveLeads() {

        Lead lead = new Lead("Lead", "lead@example.com", "123");
        lead.setUser(user);

        when(leadRepository.findByUserAndDeletedAtIsNull(user))
                .thenReturn(List.of(lead));

        List<Lead> result = leadService.listActiveLeads(user);

        assertThat(result).hasSize(1);
        verify(leadRepository).findByUserAndDeletedAtIsNull(user);
    }

    /* ==========================
       GET BY ID
       ========================== */

    @Test
    void shouldReturnLeadByIdForUser() {

        Lead lead = new Lead("Lead", "lead@example.com", "123");
        ReflectionTestUtils.setField(lead, "id", 1L);

        when(leadRepository.findByIdAndUserAndDeletedAtIsNull(1L, user))
                .thenReturn(Optional.of(lead));

        Lead result = leadService.getByIdForUser(1L, user);

        assertThat(result).isEqualTo(lead);
    }

    @Test
    void shouldThrowIfLeadNotFound() {

        when(leadRepository.findByIdAndUserAndDeletedAtIsNull(1L, user))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                leadService.getByIdForUser(1L, user)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    /* ==========================
       UPDATE STATUS
       ========================== */

    @Test
    void shouldFollowValidLifecycle() {

        Lead lead = new Lead("Lead", "lead@example.com", "123");
        lead.setStatus(LeadStatus.NEW);
        ReflectionTestUtils.setField(lead, "id", 1L);

        when(leadRepository.findByIdAndUserAndDeletedAtIsNull(1L, user))
                .thenReturn(Optional.of(lead));

        leadService.updateStatus(1L, LeadStatus.CONTACTED, user);
        leadService.updateStatus(1L, LeadStatus.QUALIFIED, user);
        leadService.updateStatus(1L, LeadStatus.CLOSED, user);

        assertThat(lead.getStatus()).isEqualTo(LeadStatus.CLOSED);

        verify(historyRepository, times(3))
                .save(any(LeadStatusHistory.class));
    }

    @Test
    void shouldBlockRegression() {

        Lead lead = new Lead("Lead", "lead@example.com", "123");
        lead.setStatus(LeadStatus.CONTACTED);

        when(leadRepository.findByIdAndUserAndDeletedAtIsNull(1L, user))
                .thenReturn(Optional.of(lead));

        assertThatThrownBy(() ->
                leadService.updateStatus(1L, LeadStatus.NEW, user)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Invalid status transition");
    }

    @Test
    void shouldBlockSkippingStages() {

        Lead lead = new Lead("Lead", "lead@example.com", "123");
        lead.setStatus(LeadStatus.NEW);

        when(leadRepository.findByIdAndUserAndDeletedAtIsNull(1L, user))
                .thenReturn(Optional.of(lead));

        assertThatThrownBy(() ->
                leadService.updateStatus(1L, LeadStatus.QUALIFIED, user)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Invalid status transition");
    }

    @Test
    void shouldNotSaveHistoryIfStatusIsSame() {

        Lead lead = new Lead("Lead", "lead@example.com", "123");
        lead.setStatus(LeadStatus.NEW);

        when(leadRepository.findByIdAndUserAndDeletedAtIsNull(1L, user))
                .thenReturn(Optional.of(lead));

        leadService.updateStatus(1L, LeadStatus.NEW, user);

        verify(historyRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUpdatingDeletedLead() {

        when(leadRepository.findByIdAndUserAndDeletedAtIsNull(1L, user))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                leadService.updateStatus(1L, LeadStatus.CONTACTED, user)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessage("Lead not found or already deleted");
    }

    /* ==========================
       SOFT DELETE
       ========================== */

    @Test
    void shouldSoftDeleteLead() {

        Lead lead = new Lead("Lead", "lead@example.com", "123");

        when(leadRepository.findByIdAndUserAndDeletedAtIsNull(1L, user))
                .thenReturn(Optional.of(lead));

        leadService.softDelete(1L, user);

        assertThat(lead.getDeletedAt()).isNotNull();
        verify(leadRepository).save(lead);
    }

    @Test
    void shouldThrowWhenDeletingLeadNotFound() {

        when(leadRepository.findByIdAndUserAndDeletedAtIsNull(1L, user))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                leadService.softDelete(1L, user)
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
