package com.leadflow.backend.repository.lead;

import com.leadflow.backend.IntegrationTestBase;
import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LeadRepositoryTest extends IntegrationTestBase {

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User createUser() {
        Role role = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(new Role("USER")));

        String randomEmail = "user_" + UUID.randomUUID() + "@example.com";

        User user = new User("Test User", randomEmail, "password", role);
        return userRepository.save(user);
    }

    @AfterEach
    void cleanup() {
        leadRepository.deleteAll();
        userRepository.deleteAll();
    }

    /* ==========================
       SAVE & FIND
       ========================== */

    @Test
    @DisplayName("Should save and retrieve a Lead entity")
    void shouldSaveAndRetrieveLead() {

        User savedUser = createUser();

        Lead lead = new Lead(
                "Test Lead",
                "lead_" + UUID.randomUUID() + "@example.com",
                "123456789"
        );
        lead.setUser(savedUser);

        Lead savedLead = leadRepository.saveAndFlush(lead);

        Optional<Lead> retrievedLead =
                leadRepository.findById(savedLead.getId());

        assertThat(retrievedLead).isPresent();
        assertThat(retrievedLead.get().getName()).isEqualTo("Test Lead");
        assertThat(retrievedLead.get().getUser().getId())
                .isEqualTo(savedUser.getId());
    }

    /* ==========================
       UNIQUE CONSTRAINT
       ========================== */

    @Test
    @DisplayName("Should not allow duplicate lead email")
    void shouldNotAllowDuplicateEmail() {

        User user = createUser();

        String email = "duplicate_" + UUID.randomUUID() + "@example.com";

        Lead lead1 = new Lead("Lead 1", email, "111");
        lead1.setUser(user);
        leadRepository.saveAndFlush(lead1);

        Lead lead2 = new Lead("Lead 2", email, "222");
        lead2.setUser(user);

        assertThatThrownBy(() ->
                leadRepository.saveAndFlush(lead2)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    /* ==========================
       FILTERS
       ========================== */

    @Test
    @DisplayName("Should find leads by user and not deleted")
    void shouldFindByUserAndDeletedAtIsNull() {

        User user = createUser();

        Lead lead1 = new Lead("Lead 1",
                "lead1_" + UUID.randomUUID() + "@example.com", "123");
        lead1.setUser(user);

        Lead lead2 = new Lead("Lead 2",
                "lead2_" + UUID.randomUUID() + "@example.com", "456");
        lead2.setUser(user);

        leadRepository.saveAll(List.of(lead1, lead2));

        List<Lead> leads =
                leadRepository.findByUserAndDeletedAtIsNull(user);

        assertThat(leads).hasSize(2);
    }

    @Test
    @DisplayName("Should exclude soft deleted leads")
    void shouldPerformSoftDelete() {

        User user = createUser();

        Lead lead = new Lead(
                "Lead",
                "soft_" + UUID.randomUUID() + "@example.com",
                "123456789"
        );
        lead.setUser(user);

        Lead savedLead = leadRepository.saveAndFlush(lead);

        savedLead.setDeletedAt(LocalDateTime.now());
        leadRepository.saveAndFlush(savedLead);

        List<Lead> leads =
                leadRepository.findByUserAndDeletedAtIsNull(user);

        assertThat(leads).isEmpty();
    }

    /* ==========================
       COUNT
       ========================== */

    @Test
    @DisplayName("Should count leads by status and not deleted")
    void shouldCountByStatusAndDeletedAtIsNull() {

        User user = createUser();

        Lead lead1 = new Lead("Lead 1",
                "c1_" + UUID.randomUUID() + "@example.com", "123");
        lead1.setUser(user);
        lead1.setStatus(LeadStatus.NEW);

        Lead lead2 = new Lead("Lead 2",
                "c2_" + UUID.randomUUID() + "@example.com", "456");
        lead2.setUser(user);
        lead2.setStatus(LeadStatus.NEW);

        Lead lead3 = new Lead("Lead 3",
                "c3_" + UUID.randomUUID() + "@example.com", "789");
        lead3.setUser(user);
        lead3.setStatus(LeadStatus.CONTACTED);

        leadRepository.saveAll(List.of(lead1, lead2, lead3));

        long count =
                leadRepository.countByStatusAndDeletedAtIsNull(LeadStatus.NEW);

        assertThat(count).isEqualTo(2);
    }

    /* ==========================
       ENUM VS DATABASE CHECK
       ========================== */

    @Test
    @DisplayName("Should fail if database constraint is inconsistent with enum")
    void shouldFailIfStatusConstraintIsInvalid() {

        User user = createUser();

        Lead lead = new Lead(
                "Invalid Status Lead",
                "invalid_" + UUID.randomUUID() + "@example.com",
                "999"
        );
        lead.setUser(user);
        lead.setStatus(LeadStatus.QUALIFIED); // possível inconsistência

        assertThatThrownBy(() ->
                leadRepository.saveAndFlush(lead)
        ).isInstanceOf(Exception.class);
    }
}
