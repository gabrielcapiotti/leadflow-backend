package com.leadflow.backend.repository.lead;

import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class LeadRepositoryTest {

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User createUser() {
        Role role = roleRepository.save(new Role("USER"));

        User user = new User(
                "Test User",
                "user@example.com",
                "password",
                role
        );

        return userRepository.save(user);
    }

    @Test
    @DisplayName("Should save and retrieve a Lead entity")
    void shouldSaveAndRetrieveLead() {

        User savedUser = createUser();

        Lead lead = new Lead("Test Lead", "test@example.com", "123456789");
        lead.setUser(savedUser);

        Lead savedLead = leadRepository.save(lead);

        Optional<Lead> retrievedLead = leadRepository.findById(savedLead.getId());

        assertThat(retrievedLead).isPresent();
        assertThat(retrievedLead.get().getName()).isEqualTo("Test Lead");
        assertThat(retrievedLead.get().getUser().getId()).isEqualTo(savedUser.getId());
    }

    @Test
    @DisplayName("Should find leads by user and not deleted")
    void shouldFindByUserAndDeletedAtIsNull() {

        User savedUser = createUser();

        Lead lead1 = new Lead("Lead 1", "lead1@example.com", "123");
        lead1.setUser(savedUser);

        Lead lead2 = new Lead("Lead 2", "lead2@example.com", "456");
        lead2.setUser(savedUser);

        leadRepository.saveAll(List.of(lead1, lead2));

        List<Lead> leads =
                leadRepository.findByUserAndDeletedAtIsNull(savedUser);

        assertThat(leads).hasSize(2);
    }

    @Test
    @DisplayName("Should find lead by id, user and not deleted")
    void shouldFindByIdAndUserAndDeletedAtIsNull() {

        User savedUser = createUser();

        Lead lead = new Lead("Lead", "lead@example.com", "123");
        lead.setUser(savedUser);

        Lead savedLead = leadRepository.save(lead);

        Optional<Lead> retrieved =
                leadRepository.findByIdAndUserAndDeletedAtIsNull(
                        savedLead.getId(),
                        savedUser
                );

        assertThat(retrieved).isPresent();
    }

    @Test
    @DisplayName("Should count leads by status and not deleted")
    void shouldCountByStatusAndDeletedAtIsNull() {

        User savedUser = createUser();

        Lead lead1 = new Lead("Lead 1", "lead1@example.com", "123");
        lead1.setUser(savedUser);
        lead1.setStatus(LeadStatus.NEW);

        Lead lead2 = new Lead("Lead 2", "lead2@example.com", "456");
        lead2.setUser(savedUser);
        lead2.setStatus(LeadStatus.NEW);

        Lead lead3 = new Lead("Lead 3", "lead3@example.com", "789");
        lead3.setUser(savedUser);
        lead3.setStatus(LeadStatus.CONTACTED);

        leadRepository.saveAll(List.of(lead1, lead2, lead3));

        long count =
                leadRepository.countByStatusAndDeletedAtIsNull(
                        LeadStatus.NEW
                );

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should perform soft delete by setting deletedAt and exclude from queries")
    void shouldPerformSoftDelete() {
        // Arrange
        User user = new User();
        user.setName("Test User");
        user.setEmail("user@example.com");
        user.setPassword("password");
        User savedUser = userRepository.save(user);

        Lead lead = new Lead("Lead", "lead@example.com", "123456789");
        lead.setUser(savedUser);
        Lead savedLead = leadRepository.save(lead);

        // Act
        savedLead.setDeletedAt(LocalDateTime.now());
        leadRepository.save(savedLead);

        List<Lead> leads = leadRepository.findByUserAndDeletedAtIsNull(savedUser);

        // Assert
        assertThat(leads).isEmpty();
    }

    @Test
    @DisplayName("Should query leads by status and user, excluding deleted")
    void shouldQueryByStatusAndUser() {
        // Arrange
        User user = new User();
        user.setName("Test User");
        user.setEmail("user@example.com");
        user.setPassword("password");
        User savedUser = userRepository.save(user);

        Lead lead1 = new Lead("Lead 1", "lead1@example.com", "123456789");
        lead1.setUser(savedUser);
        lead1.setStatus(LeadStatus.NEW);
        leadRepository.save(lead1);

        Lead lead2 = new Lead("Lead 2", "lead2@example.com", "987654321");
        lead2.setUser(savedUser);
        lead2.setStatus(LeadStatus.CONTACTED);
        leadRepository.save(lead2);

        Lead lead3 = new Lead("Lead 3", "lead3@example.com", "111222333");
        lead3.setUser(savedUser);
        lead3.setStatus(LeadStatus.NEW);
        lead3.setDeletedAt(LocalDateTime.now());
        leadRepository.save(lead3);

        // Act
        List<Lead> leads = leadRepository.findByUserAndStatusAndDeletedAtIsNull(savedUser, LeadStatus.NEW);

        // Assert
        assertThat(leads).hasSize(1);
        assertThat(leads.get(0).getName()).isEqualTo("Lead 1");
    }
}
