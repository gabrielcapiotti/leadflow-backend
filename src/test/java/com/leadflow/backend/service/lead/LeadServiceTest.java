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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
        user = new User("Test User", "test@example.com", "password", role);
    }

    /* ==========================
       CREATE
       ========================== */

    @Test
    @DisplayName("Should create lead and register history")
    void shouldCreateLead() {

        Lead savedLead = new Lead("Lead", "lead@example.com", "123");
        savedLead.setUser(user);
        ReflectionTestUtils.setField(savedLead, "id", 1L);

        when(leadRepository.save(any(Lead.class)))
                .thenReturn(savedLead);

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
                history.getLead().equals(savedLead) &&
                history.getStatus() == LeadStatus.NEW &&
                history.getChangedBy().equals(user)
        ));
    }

    @Test
    @DisplayName("Should throw exception when creating lead with null user")
    void shouldThrowWhenCreatingLeadWithNullUser() {
        assertThatThrownBy(() -> leadService.createLead("Lead", "lead@example.com", "123", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User cannot be null");
    }

    @Test
    void shouldNotAllowDuplicateEmail() {
        Lead existingLead = new Lead("Existing Lead", "duplicate@example.com", "123");
        ReflectionTestUtils.setField(existingLead, "id", 1L);

        when(leadRepository.findByUserAndDeletedAtIsNull(user))
                .thenReturn(List.of(existingLead));

        assertThatThrownBy(() ->
                leadService.createLead("New Lead", "duplicate@example.com", "456", user)
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
        lead.setUser(user);

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
    void shouldUpdateStatusAndSaveHistory() {

        Lead lead = new Lead("Lead", "lead@example.com", "123");
        lead.setUser(user);
        ReflectionTestUtils.setField(lead, "id", 1L);

        // Ensure user has an ID
        ReflectionTestUtils.setField(user, "id", 1L);

        when(leadRepository.findByIdAndUserAndDeletedAtIsNull(eq(1L), any(User.class)))
                .thenReturn(Optional.of(lead));

        Lead updated =
                leadService.updateStatus(1L, LeadStatus.CONTACTED, user);

        assertThat(updated.getStatus())
                .isEqualTo(LeadStatus.CONTACTED);

        verify(historyRepository).save(argThat(history ->
                history.getStatus() == LeadStatus.CONTACTED
        ));
    }

    @Test
    void shouldNotSaveHistoryIfStatusIsSame() {

        Lead lead = new Lead("Lead", "lead@example.com", "123");
        lead.setUser(user);
        lead.setStatus(LeadStatus.NEW);

        when(leadRepository.findByIdAndUserAndDeletedAtIsNull(1L, user))
                .thenReturn(Optional.of(lead));

        Lead result =
                leadService.updateStatus(1L, LeadStatus.NEW, user);

        assertThat(result.getStatus()).isEqualTo(LeadStatus.NEW);

        verify(historyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should not save history when status does not change")
    void shouldNotSaveHistoryWhenStatusDoesNotChange() {
        Lead mockLead = new Lead("Lead", "lead@example.com", "123");
        ReflectionTestUtils.setField(mockLead, "id", 1L);
        mockLead.setStatus(LeadStatus.NEW);

        when(leadRepository.findByIdAndUserAndDeletedAtIsNull(1L, user))
                .thenReturn(Optional.of(mockLead));

        Lead result = leadService.updateStatus(1L, LeadStatus.NEW, user);

        assertThat(result.getStatus()).isEqualTo(LeadStatus.NEW);

        verify(historyRepository, never()).save(any(LeadStatusHistory.class));
    }

    @Test
    @DisplayName("Should throw exception when updating a deleted lead")
    void shouldThrowWhenUpdatingDeletedLead() {
        Lead mockLead = new Lead("Lead", "lead@example.com", "123");
        ReflectionTestUtils.setField(mockLead, "id", 1L);
        mockLead.setDeletedAt(LocalDateTime.now());

        when(leadRepository.findByIdAndUserAndDeletedAtIsNull(1L, user))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> leadService.updateStatus(1L, LeadStatus.CONTACTED, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Lead not found or already deleted");
    }

    @Test
    void shouldValidateStatusTransition() {
        Lead lead = new Lead("Lead", "lead@example.com", "123");
        lead.setStatus(LeadStatus.NEW);
        ReflectionTestUtils.setField(lead, "id", 1L);

        when(leadRepository.findByIdAndUserAndDeletedAtIsNull(eq(1L), any(User.class)))
                .thenReturn(Optional.of(lead));

        Lead result = leadService.updateStatus(1L, LeadStatus.NEW, user);

        assertThat(result.getStatus()).isEqualTo(LeadStatus.NEW);
        verify(historyRepository, never()).save(any(LeadStatusHistory.class));
    }

    /* ==========================
       SOFT DELETE
       ========================== */

    @Test
    void shouldSoftDeleteLead() {

        Lead lead = new Lead("Lead", "lead@example.com", "123");
        lead.setUser(user);
        ReflectionTestUtils.setField(lead, "id", 1L);

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
