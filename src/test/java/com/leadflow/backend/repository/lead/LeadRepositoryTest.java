package com.leadflow.backend.repository.lead;

import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.enums.LeadStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class LeadRepositoryTest {

    @Autowired
    private LeadRepository leadRepository;

    @Test
    void shouldSaveAndFindLeadById() {
        // Arrange
        Lead lead = new Lead("Test Lead", "test@example.com", "123456789");
        lead.setStatus(LeadStatus.NEW);

        // Act
        Lead savedLead = leadRepository.save(lead);
        Optional<Lead> foundLead = leadRepository.findById(savedLead.getId());

        // Assert
        assertThat(foundLead).isPresent();
        assertThat(foundLead.get().getName()).isEqualTo("Test Lead");
        assertThat(foundLead.get().getEmail()).isEqualTo("test@example.com");
        assertThat(foundLead.get().getStatus()).isEqualTo(LeadStatus.NEW);
    }
}