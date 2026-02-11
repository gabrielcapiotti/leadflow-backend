package com.leadflow.backend.service.lead;

import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.repository.lead.LeadRepository;
import com.leadflow.backend.repository.lead.LeadStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private LeadStatusHistoryRepository statusHistoryRepository;

    @InjectMocks
    private LeadService leadService;

    private User user;
    private Lead lead;

    @BeforeEach
    void setUp() throws Exception {
        Role role = new Role("USER");
        setField(role, "id", 1);

        user = new User("Test User", "test@example.com", "password", role);
        setField(user, "id", 1L);

        lead = new Lead("Test Lead", "lead@example.com", "123456789");
        setField(lead, "id", 1L);
        setField(lead, "user", user);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void createLead_ShouldSaveAndReturnLead() {
        when(leadRepository.save(any(Lead.class))).thenReturn(lead);

        Lead result = leadService.createLead("Test Lead", "lead@example.com", "123456789", user);

        assertNotNull(result);
        assertEquals("Test Lead", result.getName());
        assertEquals("lead@example.com", result.getEmail());
        assertEquals(LeadStatus.NEW, result.getStatus());
        verify(leadRepository, times(1)).save(any(Lead.class));
        verify(statusHistoryRepository, times(1)).save(any());
    }

    @Test
    void listActiveLeads_ShouldReturnUserLeads() {
        when(leadRepository.findByUserAndDeletedAtIsNull(user)).thenReturn(Arrays.asList(lead));

        List<Lead> leads = leadService.listActiveLeads(user);

        assertNotNull(leads);
        assertEquals(1, leads.size());
        assertEquals("Test Lead", leads.get(0).getName());
        verify(leadRepository, times(1)).findByUserAndDeletedAtIsNull(user);
    }

    @Test
    void getLeadById_ShouldReturnLead_WhenLeadExists() {
        when(leadRepository.findByIdAndUserAndDeletedAtIsNull(1L, user)).thenReturn(Optional.of(lead));

        Lead result = leadService.getByIdForUser(1L, user);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Lead", result.getName());
    }

    @Test
    void getLeadById_ShouldThrowException_WhenLeadNotFound() {
        when(leadRepository.findByIdAndUserAndDeletedAtIsNull(1L, user)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> leadService.getByIdForUser(1L, user));
    }

    @Test
    void updateStatus_ShouldUpdateLeadStatus() {
        when(leadRepository.findByIdAndUserAndDeletedAtIsNull(1L, user)).thenReturn(Optional.of(lead));

        Lead result = leadService.updateStatus(1L, LeadStatus.CONTACTED, user);

        assertNotNull(result);
        verify(statusHistoryRepository, times(1)).save(any());
    }
}
