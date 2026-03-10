package com.leadflow.backend.config;

import com.leadflow.backend.entities.Plan;
import com.leadflow.backend.repository.PlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanInitializerTest {

    @Mock
    private PlanRepository planRepository;

    private PlanInitializer planInitializer;

    @BeforeEach
    void setUp() {
        planInitializer = new PlanInitializer(planRepository);
    }

    @Test
    void shouldCreatePlanWhenNoPlanExists() {
        // Arrange
        when(planRepository.count()).thenReturn(0L);
        when(planRepository.save(any(Plan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        planInitializer.initializePlan();

        // Assert
        ArgumentCaptor<Plan> planCaptor = ArgumentCaptor.forClass(Plan.class);
        verify(planRepository).save(planCaptor.capture());

        Plan savedPlan = planCaptor.getValue();
        assertEquals("Leadflow Standard", savedPlan.getName());
        assertEquals(500, savedPlan.getMaxLeads());
        assertEquals(10, savedPlan.getMaxUsers());
        assertEquals(1000, savedPlan.getMaxAiExecutions());
        assertTrue(savedPlan.getActive());
    }

    @Test
    void shouldNotCreatePlanWhenPlanAlreadyExists() {
        // Arrange
        when(planRepository.count()).thenReturn(1L);

        // Act
        planInitializer.initializePlan();

        // Assert
        verify(planRepository, never()).save(any(Plan.class));
    }

    @Test
    void shouldCreatePlanWithCorrectLimits() {
        // Arrange
        when(planRepository.count()).thenReturn(0L);

        // Act
        planInitializer.initializePlan();

        // Assert
        ArgumentCaptor<Plan> planCaptor = ArgumentCaptor.forClass(Plan.class);
        verify(planRepository).save(planCaptor.capture());

        Plan plan = planCaptor.getValue();
        assertEquals(500, plan.getMaxLeads(), "Max leads should be 500");
        assertEquals(10, plan.getMaxUsers(), "Max users should be 10");
        assertEquals(1000, plan.getMaxAiExecutions(), "Max AI executions should be 1000");
    }
}
