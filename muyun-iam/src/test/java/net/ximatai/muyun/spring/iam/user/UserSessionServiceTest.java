package net.ximatai.muyun.spring.iam.user;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.exception.AuthenticationFailedException;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserSessionServiceTest {
    private final PasswordHashingService passwordHashingService = new PasswordHashingService();
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-20T00:00:00Z"), ZoneOffset.UTC);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldCreateUserWithHashedPasswordAndLoginAsCurrentUser() {
        UserAccountDao dao = mock(UserAccountDao.class);
        when(dao.insert(any())).thenAnswer(invocation -> {
            UserAccount user = invocation.getArgument(0);
            user.setId("user-1");
            return "user-1";
        });
        UserAccountService userService = new UserAccountService(dao, tenantId -> {
        }, passwordHashingService);

        UserAccount user = new UserAccount();
        user.setUsername("alice");
        user.setTitle("Alice");
        user.setOrganizationId("org-1");
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            userService.createUser(user, "secret1");
        }

        assertThat(user.getPasswordHash()).startsWith("pbkdf2$");
        assertThat(user.getPasswordHash()).doesNotContain("secret1");

        when(dao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(user));
        UserSessionDao sessionDao = mock(UserSessionDao.class);
        AtomicReference<UserSession> persistedSession = captureInsertedSession(sessionDao);
        UserSessionService sessionService = new UserSessionService(userService, sessionDao, clock);
        LoginResult login = sessionService.login("tenant-a", "alice", "secret1");

        assertThat(login.tokenType()).isEqualTo("Bearer");
        assertThat(login.issuedAt()).isEqualTo(clock.instant());
        assertThat(login.currentUser()).isEqualTo(
                CurrentUser.tenantUser("user-1", "alice", "tenant-a", "org-1", true));
        assertThat(login.passwordChangeRequired()).isTrue();
        assertThat(login.passwordStatus()).isEqualTo(PasswordStatus.INITIAL);
        assertThat(persistedSession.get().getTenantId()).isEqualTo("tenant-a");
        assertThat(persistedSession.get().getUserId()).isEqualTo("user-1");
        assertThat(persistedSession.get().getPasswordChangeRequired()).isTrue();
        assertThat(persistedSession.get().getTokenHash()).hasSize(64);
        assertThat(persistedSession.get().getTokenHash()).isNotEqualTo(login.token());
        assertThat(persistedSession.get().getExpiresAt()).isEqualTo(clock.instant().plusSeconds(43_200));
        assertThat(persistedSession.get().getMaxExpiresAt()).isEqualTo(clock.instant().plusSeconds(604_800));
        persistedSession.get().setLastSeenAt(clock.instant().minusSeconds(120));
        when(sessionDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(persistedSession.get()));
        when(sessionDao.updateByIdAndVersion(any(UserSession.class), any())).thenReturn(1);
        assertThat(sessionService.currentUser(login.token())).contains(login.currentUser());
        assertThat(user.getLastLoginAt()).isEqualTo(clock.instant());
        assertThat(user.getFailedLoginCount()).isZero();
        assertThat(persistedSession.get().getLastSeenAt()).isEqualTo(clock.instant());
        verify(sessionDao).updateByIdAndVersion(persistedSession.get(), 0);
    }

    @Test
    void shouldNormalizeSessionTimesToDatabasePrecision() {
        Instant preciseNow = Instant.parse("2026-06-20T00:00:00.123456789Z");
        Clock preciseClock = Clock.fixed(preciseNow, ZoneOffset.UTC);
        UserAccountDao dao = mock(UserAccountDao.class);
        when(dao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(activeUser()));
        UserAccountService userService = new UserAccountService(dao, tenantId -> {
        }, passwordHashingService);
        UserSessionDao sessionDao = mock(UserSessionDao.class);
        AtomicReference<UserSession> persistedSession = captureInsertedSession(sessionDao);
        UserSessionService sessionService = new UserSessionService(userService, sessionDao, preciseClock);

        LoginResult login = sessionService.login("tenant-a", "alice", "secret1");

        Instant databaseNow = Instant.parse("2026-06-20T00:00:00.123456Z");
        assertThat(login.issuedAt()).isEqualTo(databaseNow);
        assertThat(persistedSession.get().getIssuedAt()).isEqualTo(databaseNow);
        assertThat(persistedSession.get().getLastSeenAt()).isEqualTo(databaseNow);
        assertThat(persistedSession.get().getExpiresAt()).isEqualTo(databaseNow.plusSeconds(43_200));
        assertThat(persistedSession.get().getMaxExpiresAt()).isEqualTo(databaseNow.plusSeconds(604_800));
    }

    @Test
    void shouldDropSessionWhenUserIsNoLongerActive() {
        UserAccountDao dao = mock(UserAccountDao.class);
        UserAccount enabled = activeUser();
        UserAccount disabled = activeUser();
        disabled.setEnabled(Boolean.FALSE);
        when(dao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(enabled))
                .thenReturn(List.of(disabled));
        UserAccountService userService = new UserAccountService(dao, tenantId -> {
        }, passwordHashingService);
        UserSessionDao sessionDao = mock(UserSessionDao.class);
        AtomicReference<UserSession> persistedSession = captureInsertedSession(sessionDao);
        UserSessionService sessionService = new UserSessionService(userService, sessionDao, clock);

        LoginResult login = sessionService.login("tenant-a", "alice", "secret1");

        when(sessionDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(persistedSession.get()));
        when(sessionDao.updateByIdAndVersion(any(UserSession.class), any())).thenReturn(1);
        assertThat(sessionService.currentUser(login.token())).isEmpty();
        assertThat(persistedSession.get().getRevokedAt()).isEqualTo(clock.instant());
        assertThat(persistedSession.get().getRevokedReason()).isEqualTo("user inactive");
    }

    @Test
    void shouldRejectLoginWhenTenantIsNoLongerActive() {
        UserAccountDao dao = mock(UserAccountDao.class);
        UserAccountService userService = new UserAccountService(dao, tenantId -> {
            throw new PlatformException("Tenant is not active: " + tenantId);
        }, passwordHashingService);
        UserSessionDao sessionDao = mock(UserSessionDao.class);
        UserSessionService sessionService = new UserSessionService(userService, sessionDao, clock);

        assertThatThrownBy(() -> sessionService.login("tenant-a", "alice", "secret1"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessageContaining("invalid username or password");
        verify(dao, never()).query(any(Criteria.class), any(PageRequest.class));
        verify(sessionDao, never()).insert(any());
    }

    @Test
    void shouldRevokeSessionWhenTenantIsNoLongerActive() {
        UserAccountDao dao = mock(UserAccountDao.class);
        UserAccountService userService = new UserAccountService(dao, tenantId -> {
            throw new PlatformException("Tenant is not active: " + tenantId);
        }, passwordHashingService);
        UserSessionDao sessionDao = mock(UserSessionDao.class);
        UserSession session = activeSession("session-1", "user-1");
        when(sessionDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(session));
        when(sessionDao.updateByIdAndVersion(any(UserSession.class), any())).thenReturn(1);
        UserSessionService sessionService = new UserSessionService(userService, sessionDao, clock);

        assertThat(sessionService.currentUser("token-1")).isEmpty();

        assertThat(session.getRevokedAt()).isEqualTo(clock.instant());
        assertThat(session.getRevokedReason()).isEqualTo("tenant inactive");
        verify(dao, never()).query(any(Criteria.class), any(PageRequest.class));
        verify(sessionDao).updateByIdAndVersion(session, 0);
    }

    @Test
    void shouldRejectInvalidPasswordWithoutIssuingSession() {
        UserAccountDao dao = mock(UserAccountDao.class);
        UserAccount user = new UserAccount();
        user.setId("user-1");
        user.setTenantId("tenant-a");
        user.setUsername("alice");
        user.setTitle("Alice");
        user.setOrganizationId("org-1");
        user.setEnabled(Boolean.TRUE);
        user.setPasswordHash(passwordHashingService.hash("secret1"));
        when(dao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(user));
        UserAccountService userService = new UserAccountService(dao, tenantId -> {
        }, passwordHashingService);
        UserSessionDao sessionDao = mock(UserSessionDao.class);
        UserSessionService sessionService = new UserSessionService(userService, sessionDao, clock);

        assertThatThrownBy(() -> sessionService.login("tenant-a", "alice", "wrong-password"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessageContaining("invalid username or password");
        assertThat(user.getLastFailedLoginAt()).isEqualTo(clock.instant());
        assertThat(user.getFailedLoginCount()).isEqualTo(1);
        verify(dao).updateByIdAndVersion(user, 0);
        verify(sessionDao, never()).insert(any());
    }

    @Test
    void shouldReturnPasswordChangeRequiredForInitialPassword() {
        UserAccount user = activeUser();
        user.setPasswordStatus(PasswordStatus.INITIAL);
        UserAccountDao dao = mock(UserAccountDao.class);
        when(dao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(user));
        UserAccountService userService = new UserAccountService(dao, tenantId -> {
        }, passwordHashingService);
        UserSessionDao sessionDao = mock(UserSessionDao.class);
        captureInsertedSession(sessionDao);
        UserSessionService sessionService = new UserSessionService(userService, sessionDao, clock);

        LoginResult login = sessionService.login("tenant-a", "alice", "secret1", "127.0.0.1", "Browser");

        assertThat(login.passwordChangeRequired()).isTrue();
        assertThat(login.passwordStatus()).isEqualTo(PasswordStatus.INITIAL);
        assertThat(user.getLastLoginIp()).isEqualTo("127.0.0.1");
        assertThat(user.getLastLoginUserAgent()).isEqualTo("Browser");
    }

    @Test
    void shouldRejectExpiredResetPasswordWithoutIssuingSession() {
        UserAccount user = activeUser();
        user.setPasswordStatus(PasswordStatus.RESET_REQUIRED);
        user.setPasswordExpiresAt(clock.instant().minusSeconds(1));
        UserAccountDao dao = mock(UserAccountDao.class);
        when(dao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(user));
        UserAccountService userService = new UserAccountService(dao, tenantId -> {
        }, passwordHashingService);
        UserSessionDao sessionDao = mock(UserSessionDao.class);
        UserSessionService sessionService = new UserSessionService(userService, sessionDao, clock);

        assertThatThrownBy(() -> sessionService.login("tenant-a", "alice", "secret1"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessageContaining("temporary password expired");

        assertThat(user.getLastFailedLoginAt()).isEqualTo(clock.instant());
        verify(sessionDao, never()).insert(any());
    }

    @Test
    void shouldNotLoginTenantUserFromSystemWorkspace() {
        UserAccountDao dao = mock(UserAccountDao.class);
        when(dao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of());
        UserAccountService userService = new UserAccountService(dao, tenantId -> {
        }, passwordHashingService);
        UserSessionDao sessionDao = mock(UserSessionDao.class);
        UserSessionService sessionService = new UserSessionService(userService, sessionDao, clock);

        assertThatThrownBy(() -> sessionService.login(null, "admin", "secret1"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessageContaining("invalid username or password");

        verify(sessionDao, never()).insert(any());
    }

    @Test
    void shouldRejectExpiredSession() {
        UserAccountDao dao = mock(UserAccountDao.class);
        UserAccountService userService = new UserAccountService(dao, tenantId -> {
        }, passwordHashingService);
        UserSessionDao sessionDao = mock(UserSessionDao.class);
        UserSession expired = activeSession("session-1", "user-1");
        expired.setExpiresAt(clock.instant().minusSeconds(1));
        when(sessionDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(expired));
        UserSessionService sessionService = new UserSessionService(userService, sessionDao, clock);

        assertThat(sessionService.currentUser("token-1")).isEmpty();
        verify(dao, never()).query(any(Criteria.class), any(PageRequest.class));
        verify(sessionDao, never()).updateByIdAndVersion(any(UserSession.class), any());
    }

    @Test
    void shouldExtendSessionIdleExpirationOnAccessWithoutPassingAbsoluteExpiration() {
        Clock accessClock = Clock.fixed(Instant.parse("2026-06-20T10:00:00Z"), ZoneOffset.UTC);
        UserAccountDao dao = mock(UserAccountDao.class);
        when(dao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(activeUser()));
        UserAccountService userService = new UserAccountService(dao, tenantId -> {
        }, passwordHashingService);
        UserSessionDao sessionDao = mock(UserSessionDao.class);
        UserSession session = activeSession("session-1", "user-1");
        session.setIssuedAt(Instant.parse("2026-06-20T00:00:00Z"));
        session.setVersion(3);
        session.setLastSeenAt(accessClock.instant().minusSeconds(120));
        session.setExpiresAt(accessClock.instant().plusSeconds(600));
        session.setMaxExpiresAt(accessClock.instant().plusSeconds(3600));
        when(sessionDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(session));
        when(sessionDao.updateByIdAndVersion(any(UserSession.class), any())).thenReturn(1);
        UserSessionService sessionService = new UserSessionService(userService, sessionDao, accessClock);

        assertThat(sessionService.currentUser("token-1")).contains(
                CurrentUser.tenantUser("user-1", "alice", "tenant-a", "org-1"));

        assertThat(session.getLastSeenAt()).isEqualTo(accessClock.instant());
        assertThat(session.getExpiresAt()).isEqualTo(session.getMaxExpiresAt());
        verify(sessionDao).updateByIdAndVersion(session, 3);
    }

    @Test
    void shouldResolveRestrictedCurrentUserFromPasswordChangeRequiredSession() {
        Clock accessClock = Clock.fixed(Instant.parse("2026-06-20T10:00:00Z"), ZoneOffset.UTC);
        UserAccountDao dao = mock(UserAccountDao.class);
        when(dao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(activeUser()));
        UserAccountService userService = new UserAccountService(dao, tenantId -> {
        }, passwordHashingService);
        UserSessionDao sessionDao = mock(UserSessionDao.class);
        UserSession session = activeSession("session-1", "user-1");
        session.setPasswordChangeRequired(Boolean.TRUE);
        session.setIssuedAt(Instant.parse("2026-06-20T00:00:00Z"));
        session.setLastSeenAt(accessClock.instant());
        session.setExpiresAt(accessClock.instant().plusSeconds(600));
        session.setMaxExpiresAt(accessClock.instant().plusSeconds(3600));
        when(sessionDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(session));
        UserSessionService sessionService = new UserSessionService(userService, sessionDao, accessClock);

        assertThat(sessionService.currentUser("token-1")).contains(
                CurrentUser.tenantUser("user-1", "alice", "tenant-a", "org-1", true));
    }

    @Test
    void shouldRejectCurrentUserWhenLastSeenRefreshConflictsWithConcurrentRevoke() {
        Clock accessClock = Clock.fixed(Instant.parse("2026-06-20T10:00:00Z"), ZoneOffset.UTC);
        UserAccountDao dao = mock(UserAccountDao.class);
        when(dao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(activeUser()));
        UserAccountService userService = new UserAccountService(dao, tenantId -> {
        }, passwordHashingService);
        UserSessionDao sessionDao = mock(UserSessionDao.class);
        UserSession stale = activeSession("session-1", "user-1");
        stale.setVersion(5);
        stale.setExpiresAt(accessClock.instant().plusSeconds(3600));
        stale.setMaxExpiresAt(accessClock.instant().plusSeconds(7200));
        stale.setLastSeenAt(accessClock.instant().minusSeconds(120));
        UserSession revoked = activeSession("session-1", "user-1");
        revoked.setVersion(6);
        revoked.setExpiresAt(accessClock.instant().plusSeconds(3600));
        revoked.setMaxExpiresAt(accessClock.instant().plusSeconds(7200));
        revoked.setRevokedAt(accessClock.instant().minusSeconds(1));
        revoked.setRevokedReason("logout");
        when(sessionDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(stale))
                .thenReturn(List.of(revoked));
        when(sessionDao.updateByIdAndVersion(any(UserSession.class), any())).thenReturn(0);
        UserSessionService sessionService = new UserSessionService(userService, sessionDao, accessClock);

        assertThat(sessionService.currentUser("token-1")).isEmpty();

        verify(sessionDao).updateByIdAndVersion(stale, 5);
    }

    @Test
    void shouldAllowMultipleSessionsAndRevokeAllUserSessions() {
        UserAccountDao dao = mock(UserAccountDao.class);
        UserAccountService userService = new UserAccountService(dao, tenantId -> {
        }, passwordHashingService);
        UserSessionDao sessionDao = mock(UserSessionDao.class);
        UserSession web = activeSession("session-web", "user-1");
        UserSession mobile = activeSession("session-mobile", "user-1");
        UserSession revoked = activeSession("session-old", "user-1");
        revoked.setRevokedAt(clock.instant().minusSeconds(60));
        revoked.setRevokedReason("logout");
        when(sessionDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(web, mobile, revoked));
        when(sessionDao.updateByIdAndVersion(any(UserSession.class), any())).thenReturn(1);
        UserSessionService sessionService = new UserSessionService(userService, sessionDao, clock);

        sessionService.revokeUserSessions("user-1");

        assertThat(web.getRevokedReason()).isEqualTo("user sessions revoked");
        assertThat(mobile.getRevokedReason()).isEqualTo("user sessions revoked");
        assertThat(revoked.getRevokedReason()).isEqualTo("logout");
        verify(sessionDao).updateByIdAndVersion(web, 0);
        verify(sessionDao).updateByIdAndVersion(mobile, 0);
    }

    @Test
    void shouldTreatMalformedPasswordHashAsNotMatched() {
        assertThat(passwordHashingService.matches("secret1", "pbkdf2$bad$not-base64")).isFalse();
        assertThat(passwordHashingService.matches("secret1", "pbkdf2$1$a$b")).isFalse();
    }

    private UserAccount activeUser() {
        UserAccount user = new UserAccount();
        user.setId("user-1");
        user.setTenantId("tenant-a");
        user.setUsername("alice");
        user.setTitle("Alice");
        user.setOrganizationId("org-1");
        user.setEnabled(Boolean.TRUE);
        user.setPasswordHash(passwordHashingService.hash("secret1"));
        return user;
    }

    private AtomicReference<UserSession> captureInsertedSession(UserSessionDao sessionDao) {
        AtomicReference<UserSession> reference = new AtomicReference<>();
        when(sessionDao.insert(any())).thenAnswer(invocation -> {
            UserSession session = invocation.getArgument(0);
            reference.set(session);
            return session.getId();
        });
        return reference;
    }

    private UserSession activeSession(String id, String userId) {
        UserSession session = new UserSession();
        session.setId(id);
        session.setTenantId("tenant-a");
        session.setUserId(userId);
        session.setUsername("alice");
        session.setOrganizationId("org-1");
        session.setTokenHash("hash-" + id);
        session.setIssuedAt(clock.instant());
        session.setExpiresAt(clock.instant().plusSeconds(3600));
        session.setMaxExpiresAt(clock.instant().plusSeconds(604_800));
        session.setLastSeenAt(clock.instant());
        return session;
    }
}
