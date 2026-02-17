-- V4__update_lead_status_constraint.sql

ALTER TABLE leads
    DROP CONSTRAINT IF EXISTS chk_lead_status;

ALTER TABLE leads
    ADD CONSTRAINT chk_lead_status
    CHECK (status IN ('NEW', 'CONTACTED', 'QUALIFIED', 'CLOSED'));

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

}
