package com.leadflow.backend.service.vendor;

import com.leadflow.backend.entities.Plan;
import com.leadflow.backend.entities.UsageLimit;
import com.leadflow.backend.exception.PlanLimitExceededException;
import com.leadflow.backend.repository.UsageLimitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsageServiceTest {

    @Mock
    private UsageLimitRepository usageRepository;

    @InjectMocks
    private UsageService usageService;

    private UUID tenantId;
    private Plan plan;
    private UsageLimit usage;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        
        plan = new Plan();
        plan.setName("Test Plan");
        plan.setMaxLeads(500);
        plan.setMaxUsers(10);
        plan.setMaxAiExecutions(1000);
        plan.setActive(true);

        usage = new UsageLimit();
        usage.setTenantId(tenantId);
        usage.setPlan(plan);
        usage.setLeadsUsed(0);
        usage.setUsersUsed(0);
        usage.setAiExecutionsUsed(0);
    }

    @Test
    void shouldInitializeUsageWhenTenantDoesNotExist() {
        // Arrange
        when(usageRepository.existsByTenantId(tenantId)).thenReturn(false);
        when(usageRepository.save(any(UsageLimit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UsageLimit result = usageService.initializeUsage(tenantId, plan);

        // Assert
        ArgumentCaptor<UsageLimit> captor = ArgumentCaptor.forClass(UsageLimit.class);
        verify(usageRepository).save(captor.capture());

        UsageLimit savedUsage = captor.getValue();
        assertNotNull(result);
        assertNotNull(savedUsage);
        assertEquals(tenantId, savedUsage.getTenantId());
        assertEquals(plan, savedUsage.getPlan());
        assertEquals(0, savedUsage.getLeadsUsed());
        assertEquals(0, savedUsage.getUsersUsed());
        assertEquals(0, savedUsage.getAiExecutionsUsed());
    }

    @Test
    void shouldNotInitializeUsageWhenTenantAlreadyExists() {
        // Arrange
        when(usageRepository.existsByTenantId(tenantId)).thenReturn(true);
        when(usageRepository.findByTenantId(tenantId)).thenReturn(Optional.of(usage));

        // Act
        UsageLimit result = usageService.initializeUsage(tenantId, plan);

        // Assert
        assertEquals(usage, result);
        verify(usageRepository, never()).save(any(UsageLimit.class));
    }

    @Test
    void shouldGetUsageForTenant() {
        when(usageRepository.findByTenantId(tenantId)).thenReturn(Optional.of(usage));

        UsageLimit result = usageService.getUsage(tenantId);

        assertEquals(usage, result);
    }

    @Test
    void shouldThrowExceptionWhenTenantIdIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                () -> usageService.initializeUsage(null, plan));
        
        verify(usageRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenPlanIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                () -> usageService.initializeUsage(tenantId, null));
        
        verify(usageRepository, never()).save(any());
    }

    @Test
    void shouldAllowLeadCreationWhenBelowLimit() {
        usage.setLeadsUsed(499);
        when(usageRepository.findByTenantId(tenantId)).thenReturn(Optional.of(usage));

        assertDoesNotThrow(() -> usageService.validateLeadLimit(tenantId));
    }

    @Test
    void shouldThrowWhenLeadLimitReached() {
        usage.setLeadsUsed(500);
        when(usageRepository.findByTenantId(tenantId)).thenReturn(Optional.of(usage));

        PlanLimitExceededException exception = assertThrows(PlanLimitExceededException.class,
                () -> usageService.validateLeadLimit(tenantId));

        assertEquals("Lead limit reached", exception.getMessage());
    }

    @Test
    void shouldAllowUserCreationWhenBelowLimit() {
        usage.setUsersUsed(9);
        when(usageRepository.findByTenantId(tenantId)).thenReturn(Optional.of(usage));

        assertDoesNotThrow(() -> usageService.validateUserLimit(tenantId));
    }

    @Test
    void shouldThrowWhenUserLimitReached() {
        usage.setUsersUsed(10);
        when(usageRepository.findByTenantId(tenantId)).thenReturn(Optional.of(usage));

        PlanLimitExceededException exception = assertThrows(PlanLimitExceededException.class,
                () -> usageService.validateUserLimit(tenantId));

        assertEquals("User limit reached", exception.getMessage());
    }

    @Test
    void shouldAllowAiExecutionWhenBelowLimit() {
        usage.setAiExecutionsUsed(999);
        when(usageRepository.findByTenantId(tenantId)).thenReturn(Optional.of(usage));

        assertDoesNotThrow(() -> usageService.validateAiUsage(tenantId));
    }

    @Test
    void shouldThrowWhenAiLimitReached() {
        usage.setAiExecutionsUsed(1000);
        when(usageRepository.findByTenantId(tenantId)).thenReturn(Optional.of(usage));

        PlanLimitExceededException exception = assertThrows(PlanLimitExceededException.class,
                () -> usageService.validateAiUsage(tenantId));

        assertEquals("AI execution limit reached", exception.getMessage());
    }

    @Test
    void shouldThrowWhenUsageNotFound() {
        when(usageRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> usageService.validateLeadLimit(tenantId));

        assertEquals("Usage configuration not found for tenant " + tenantId, exception.getMessage());
    }

    /* =========================================================
       ATOMIC CONSUME TESTS (Professional SaaS Pattern)
       ========================================================= */

    @Test
    void shouldConsumeLeadAtomically() {
        usage.setLeadsUsed(3);
        when(usageRepository.findByTenantId(tenantId)).thenReturn(Optional.of(usage));

        usageService.consumeLead(tenantId);

        assertEquals(4, usage.getLeadsUsed());
        verify(usageRepository).save(usage);
    }

    @Test
    void shouldThrowWhenConsumeLeadExceedsLimit() {
        usage.setLeadsUsed(500);
        when(usageRepository.findByTenantId(tenantId)).thenReturn(Optional.of(usage));

        PlanLimitExceededException exception = assertThrows(PlanLimitExceededException.class,
                () -> usageService.consumeLead(tenantId));

        assertEquals("Lead limit reached", exception.getMessage());
        verify(usageRepository, never()).save(any());
    }

    @Test
    void shouldConsumeUserAtomically() {
        usage.setUsersUsed(2);
        when(usageRepository.findByTenantId(tenantId)).thenReturn(Optional.of(usage));

        usageService.consumeUser(tenantId);

        assertEquals(3, usage.getUsersUsed());
        verify(usageRepository).save(usage);
    }

    @Test
    void shouldThrowWhenConsumeUserExceedsLimit() {
        usage.setUsersUsed(10);
        when(usageRepository.findByTenantId(tenantId)).thenReturn(Optional.of(usage));

        PlanLimitExceededException exception = assertThrows(PlanLimitExceededException.class,
                () -> usageService.consumeUser(tenantId));

        assertEquals("User limit reached", exception.getMessage());
        verify(usageRepository, never()).save(any());
    }

    @Test
    void shouldConsumeAiExecutionAtomically() {
        usage.setAiExecutionsUsed(7);
        when(usageRepository.findByTenantId(tenantId)).thenReturn(Optional.of(usage));

        usageService.consumeAiExecution(tenantId);

        assertEquals(8, usage.getAiExecutionsUsed());
        verify(usageRepository).save(usage);
    }

    @Test
    void shouldThrowWhenConsumeAiExecutionExceedsLimit() {
        usage.setAiExecutionsUsed(1000);
        when(usageRepository.findByTenantId(tenantId)).thenReturn(Optional.of(usage));

        PlanLimitExceededException exception = assertThrows(PlanLimitExceededException.class,
                () -> usageService.consumeAiExecution(tenantId));

        assertEquals("AI execution limit reached", exception.getMessage());
        verify(usageRepository, never()).save(any());
    }

    /* =========================================================
       LEGACY INCREMENT TESTS (backward compatibility)
       ========================================================= */

    @Test
    void shouldIncrementLeadsUsed() {
        usage.setLeadsUsed(3);
        when(usageRepository.findByTenantId(tenantId)).thenReturn(Optional.of(usage));

        usageService.incrementLeadsUsed(tenantId);

        assertEquals(4, usage.getLeadsUsed());
        verify(usageRepository).save(usage);
    }

    @Test
    void shouldIncrementUsersUsed() {
        usage.setUsersUsed(2);
        when(usageRepository.findByTenantId(tenantId)).thenReturn(Optional.of(usage));

        usageService.incrementUsersUsed(tenantId);

        assertEquals(3, usage.getUsersUsed());
        verify(usageRepository).save(usage);
    }

    @Test
    void shouldIncrementAiExecutionsUsed() {
        usage.setAiExecutionsUsed(7);
        when(usageRepository.findByTenantId(tenantId)).thenReturn(Optional.of(usage));

        usageService.incrementAiExecutionsUsed(tenantId);

        assertEquals(8, usage.getAiExecutionsUsed());
        verify(usageRepository).save(usage);
    }
}
