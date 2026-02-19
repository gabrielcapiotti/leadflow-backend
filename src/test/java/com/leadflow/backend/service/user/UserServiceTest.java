package com.leadflow.backend.service.user;

import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

class UserServiceTest {

    private UUID userId;
    private UUID roleId;

    private User user;

    @BeforeEach
    void setUp() {

        userId = UUID.randomUUID();
        roleId = UUID.randomUUID();

        // ===== ROLE =====
        Role role = new Role("USER");
        ReflectionTestUtils.setField(role, "id", roleId);

        // ===== TENANT =====
        Tenant tenant = new Tenant(
                "Test Tenant",
                "test_schema"
        );

        // ===== USER =====
        user = new User(
                "Test User",
                "test@example.com",
                "password",
                role,
                tenant
        );

        ReflectionTestUtils.setField(user, "id", userId);
    }
}
