package com.leadflow.backend.service.auth;

import com.leadflow.backend.entities.auth.UserSession;
import com.leadflow.backend.repository.auth.UserSessionRepository;
import com.leadflow.backend.security.exception.UnauthorizedException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSessionServiceTest {

    @Mock
    private UserSessionRepository repository;

    private Clock clock;
    private UserSessionService service;

    private final UUID USER_ID = UUID.randomUUID();
    private final UUID TENANT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(
                Instant.parse("2024-01-01T10:00:00Z"),
                ZoneOffset.UTC
        );

        service = new UserSessionService(
                repository,
                clock,
                3,   // max devices
                30   // idle timeout minutes
        );
    }

    /* ====================================================== */
    /* CREATE SESSION                                         */
    /* ====================================================== */

    @Test
    void shouldCreateSession() {
        when(repository.countByUserIdAndTenantIdAndActiveTrue(USER_ID, TENANT_ID)).thenReturn(0L);

        service.createSession(
                USER_ID,
                TENANT_ID,
                "token123",
                "1.1.1.1",
                "Chrome"
        );

        verify(repository).save(any(UserSession.class));
        verify(repository).countByUserIdAndTenantIdAndActiveTrue(USER_ID, TENANT_ID);
    }

    /* ====================================================== */
    /* REVOKE SESSION                                         */
    /* ====================================================== */

    @Test
    void shouldRevokeSession() {

        Instant now = Instant.now(clock);

        UserSession session = new UserSession(
                USER_ID,
                TENANT_ID,
                "token123",
                "1.1.1.1",
                "Chrome",
                now
        );

        when(repository.findByTokenIdAndTenantIdAndActiveTrue("token123", TENANT_ID))
                .thenReturn(Optional.of(Objects.requireNonNull(session)));

        service.revokeSession("token123", TENANT_ID);

        assertThat(session.isActive()).isFalse();
        assertThat(session.getRevokedAt()).isEqualTo(now);
    }

    /* ====================================================== */
    /* INVALID TOKEN                                          */
    /* ====================================================== */

    @Test
    void shouldThrowWhenSessionNotFound() {

        when(repository.findByTokenIdAndTenantIdAndActiveTrue("invalid", TENANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.revokeSession("invalid", TENANT_ID)
        ).isInstanceOf(UnauthorizedException.class);
    }

    /* ====================================================== */
    /* DEVICE LIMIT                                           */
    /* ====================================================== */

    @Test
    void shouldRevokeOldestWhenDeviceLimitExceeded() {

        Instant now = Instant.now(clock);

        UserSession oldSession = new UserSession(
                USER_ID,
                TENANT_ID,
                "oldToken",
                "1.1.1.1",
                "Chrome",
                now.minusSeconds(3600)
        );

        when(repository.countByUserIdAndTenantIdAndActiveTrue(USER_ID, TENANT_ID))
                .thenReturn(3L);

        when(repository.findByUserIdAndTenantIdAndActiveTrueOrderByCreatedAtAsc(
                USER_ID, TENANT_ID))
                .thenReturn(List.of(oldSession));

        service.createSession(
                USER_ID,
                TENANT_ID,
                "newToken",
                "2.2.2.2",
                "Firefox"
        );

        assertThat(oldSession.isActive()).isFalse();
        assertThat(oldSession.getRevokedAt()).isEqualTo(now);
    }

    /* ====================================================== */
    /* SESSION ACTIVITY VALID                                 */
    /* ====================================================== */

    @Test
    void shouldUpdateActivityWhenSessionIsValid() {

        Instant now = Instant.now(clock);

        UserSession session = new UserSession(
                USER_ID,
                TENANT_ID,
                "token123",
                "1.1.1.1",
                "Chrome",
                now.minusSeconds(60)
        );

        when(repository.findByTokenIdAndTenantIdAndActiveTrue("token123", TENANT_ID))
                .thenReturn(Optional.of(session));

        service.processSessionActivity(
                "token123",
                TENANT_ID,
                "1.1.1.1",
                "Chrome"
        );

        assertThat(session.getLastAccessAt()).isEqualTo(now);
        assertThat(session.isActive()).isTrue();
    }

    /* ====================================================== */
    /* IDLE TIMEOUT                                           */
    /* ====================================================== */

    @Test
    void shouldRevokeSessionWhenIdleTimeoutExceeded() {

        Instant now = Instant.now(clock);

        UserSession session = new UserSession(
                USER_ID,
                TENANT_ID,
                "token123",
                "1.1.1.1",
                "Chrome",
                now.minusSeconds(2400) // 40 min
        );

        when(repository.findByTokenIdAndTenantIdAndActiveTrue("token123", TENANT_ID))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() ->
                service.processSessionActivity(
                        "token123",
                        TENANT_ID,
                        "1.1.1.1",
                        "Chrome"
                )
        ).isInstanceOf(UnauthorizedException.class);

        assertThat(session.isActive()).isFalse();
        assertThat(session.getRevokedAt()).isEqualTo(now);
    }

    /* ====================================================== */
    /* SUSPICIOUS — IP CHANGE                                 */
    /* ====================================================== */

    @Test
    void shouldMarkSessionSuspiciousWhenIpChanges() {

        Instant now = Instant.now(clock);

        UserSession session = new UserSession(
                USER_ID,
                TENANT_ID,
                "token123",
                "1.1.1.1",
                "Chrome",
                now.minusSeconds(60)
        );

        when(repository.findByTokenIdAndTenantIdAndActiveTrue("token123", TENANT_ID))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() ->
                service.processSessionActivity(
                        "token123",
                        TENANT_ID,
                        "2.2.2.2",
                        "Chrome"
                )
        ).isInstanceOf(UnauthorizedException.class);

        assertThat(session.isSuspicious()).isTrue();
    }

    /* ====================================================== */
    /* VALIDATE ACTIVE SESSION                                */
    /* ====================================================== */

    @Test
    void shouldValidateActiveSession() {

        when(repository.existsByTokenIdAndTenantIdAndActiveTrue("token123", TENANT_ID))
                .thenReturn(true);

        service.validateActiveSession("token123", TENANT_ID);

        verify(repository).existsByTokenIdAndTenantIdAndActiveTrue("token123", TENANT_ID);
    }

    /* ====================================================== */
    /* REVOKE SPECIFIC SESSION                                */
    /* ====================================================== */

    @Test
    void shouldRevokeSpecificSession() {

        Instant now = Instant.now(clock);

        UUID sessionId = UUID.randomUUID();

        UserSession session = new UserSession(
                USER_ID,
                TENANT_ID,
                "token123",
                "1.1.1.1",
                "Chrome",
                now
        );

        // 🔒 IMPORTANTE: usar o mesmo ID
        when(repository.findByIdAndUserIdAndTenantIdAndActiveTrue(
                eq(sessionId),
                eq(USER_ID),
                eq(TENANT_ID)))
                .thenReturn(Optional.of(session));

        service.revokeSpecificSession(
                sessionId,
                USER_ID,
                TENANT_ID
        );

        assertThat(session.isActive()).isFalse();
        assertThat(session.getRevokedAt()).isEqualTo(now);
    }
}