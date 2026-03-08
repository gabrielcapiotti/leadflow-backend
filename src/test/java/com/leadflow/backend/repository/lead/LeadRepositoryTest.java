package com.leadflow.backend.repository.lead;

import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LeadRepositoryTest {

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private static final String TENANT_SCHEMA = "public";

    @BeforeEach
    void setTenant() {
        TenantContext.setTenant(TENANT_SCHEMA);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    /* ======================================================
       HELPER
       ====================================================== */

    private User createUser() {

        Role role = roleRepository.findByNameIgnoreCase("ROLE_USER")
                .orElseGet(() -> roleRepository.saveAndFlush(new Role("ROLE_USER")));

        String randomEmail =
                "user_" + UUID.randomUUID() + "@example.com";

        User user = new User(
                "Test User",
                randomEmail,
                "password",
                role
        );

        return userRepository.saveAndFlush(user);
    }

    /* ======================================================
       SAVE & FIND
       ====================================================== */

    @Test
    void shouldSaveAndRetrieveLead() {

        User savedUser = createUser();

        Lead lead = new Lead(
                savedUser.getId(),
                "Test Lead",
                "lead_" + UUID.randomUUID() + "@example.com",
                "123456789"
        );

        Lead savedLead = leadRepository.saveAndFlush(lead);

        UUID safeLeadId = Objects.requireNonNull(savedLead.getId());

        Optional<Lead> retrievedLead =
                leadRepository.findById(safeLeadId);

        assertThat(retrievedLead).isPresent();
        assertThat(retrievedLead.get().getName())
                .isEqualTo("Test Lead");
        assertThat(retrievedLead.get().getUserId())
                .isEqualTo(savedUser.getId());
    }

    /* ======================================================
       UNIQUE CONSTRAINT
       ====================================================== */

    @Test
    void shouldNotAllowDuplicateEmailPerUser() {

        User user = createUser();

        String email =
                "duplicate_" + UUID.randomUUID() + "@example.com";

        Lead lead1 = new Lead(user.getId(), "Lead 1", email, "111");
        leadRepository.saveAndFlush(lead1);

        Lead lead2 = new Lead(user.getId(), "Lead 2", email, "222");

        assertThatThrownBy(() ->
                leadRepository.saveAndFlush(lead2)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    /* ======================================================
       FILTERS
       ====================================================== */

    @Test
    void shouldFindByUserIdAndDeletedAtIsNull() {

        User user = createUser();

        Lead lead1 = new Lead(
                user.getId(),
                "Lead 1",
                "lead1_" + UUID.randomUUID() + "@example.com",
                "123"
        );

        Lead lead2 = new Lead(
                user.getId(),
                "Lead 2",
                "lead2_" + UUID.randomUUID() + "@example.com",
                "456"
        );

        leadRepository.saveAllAndFlush(Objects.requireNonNull(List.of(lead1, lead2)));

        List<Lead> leads =
                leadRepository.findByUserIdAndDeletedAtIsNull(user.getId());

        assertThat(leads).hasSize(2);
    }

    @Test
    void shouldExcludeSoftDeletedLeads() {

        User user = createUser();

        Lead lead = new Lead(
                user.getId(),
                "Lead",
                "soft_" + UUID.randomUUID() + "@example.com",
                "123456789"
        );

        Lead savedLead = leadRepository.saveAndFlush(lead);

        savedLead.softDelete();
        leadRepository.saveAndFlush(savedLead);

        List<Lead> leads =
                leadRepository.findByUserIdAndDeletedAtIsNull(user.getId());

        assertThat(leads).isEmpty();
    }

    /* ======================================================
       COUNT
       ====================================================== */

    @Test
    void shouldCountByStatusAndDeletedAtIsNull() {

        User user = createUser();

        Lead lead1 = new Lead(user.getId(), "Lead 1",
                "c1_" + UUID.randomUUID() + "@example.com", "123");

        Lead lead2 = new Lead(user.getId(), "Lead 2",
                "c2_" + UUID.randomUUID() + "@example.com", "456");

        Lead lead3 = new Lead(user.getId(), "Lead 3",
                "c3_" + UUID.randomUUID() + "@example.com", "789");

        lead2.changeStatus(LeadStatus.CONTACTED);
        lead3.changeStatus(LeadStatus.CONTACTED);

        leadRepository.saveAllAndFlush(
                Objects.requireNonNull(List.of(lead1, lead2, lead3))
        );

        long count =
                leadRepository.countByStatusAndDeletedAtIsNull(
                        LeadStatus.CONTACTED
                );

        assertThat(count).isEqualTo(2);
    }
}