package com.leadflow.backend.service;

import com.leadflow.backend.entities.Plan;
import com.leadflow.backend.repository.PlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    private PlanService planService;

    @BeforeEach
    void setUp() {
        planService = new PlanService(planRepository);
    }

    @Test
    void shouldReturnActivePlanWhenExists() {
        // Arrange
        Plan expectedPlan = new Plan();
        expectedPlan.setName("Leadflow Standard");
        expectedPlan.setMaxLeads(500);
        expectedPlan.setMaxUsers(10);
        expectedPlan.setMaxAiExecutions(1000);
        expectedPlan.setActive(true);

        when(planRepository.findByActiveTrue()).thenReturn(Optional.of(expectedPlan));

        // Act
        Plan actualPlan = planService.getActivePlan();

        // Assert
        assertNotNull(actualPlan);
        assertEquals("Leadflow Standard", actualPlan.getName());
        assertEquals(500, actualPlan.getMaxLeads());
        assertEquals(10, actualPlan.getMaxUsers());
        assertEquals(1000, actualPlan.getMaxAiExecutions());
        assertTrue(actualPlan.getActive());
    }

    @Test
    void shouldThrowRuntimeExceptionWhenNoPlanExists() {
        // Arrange
        when(planRepository.findByActiveTrue()).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            planService.getActivePlan();
        });

        assertEquals("Active plan not configured", exception.getMessage());
    }

    @Test
    void shouldReturnPlanWithCorrectLimits() {
        // Arrange
        Plan plan = new Plan();
        plan.setMaxLeads(500);
        plan.setMaxUsers(10);
        plan.setMaxAiExecutions(1000);
        
        when(planRepository.findByActiveTrue()).thenReturn(Optional.of(plan));

        // Act
        Plan result = planService.getActivePlan();

        // Assert
        assertEquals(500, result.getMaxLeads());
        assertEquals(10, result.getMaxUsers());
        assertEquals(1000, result.getMaxAiExecutions());
    }
}
