package net.ximatai.muyun.spring.iam.user;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserSessionServiceTest {
    private final PasswordHashingService passwordHashingService = new PasswordHashingService();

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
        UserSessionService sessionService = new UserSessionService(userService);
        LoginResult login = sessionService.login("tenant-a", "alice", "secret1");

        assertThat(login.tokenType()).isEqualTo("Bearer");
        assertThat(login.currentUser()).isEqualTo(CurrentUser.tenantUser("user-1", "alice", "tenant-a", "org-1"));
        assertThat(sessionService.currentUser(login.token())).contains(login.currentUser());
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
        UserSessionService sessionService = new UserSessionService(userService);

        LoginResult login = sessionService.login("tenant-a", "alice", "secret1");

        assertThat(sessionService.currentUser(login.token())).isEmpty();
    }

    @Test
    void shouldRejectInvalidPasswordWithoutIssuingSession() {
        UserAccountDao dao = mock(UserAccountDao.class);
        UserAccount user = new UserAccount();
        user.setId("user-1");
        user.setTenantId("tenant-a");
        user.setUsername("alice");
        user.setTitle("Alice");
        user.setEnabled(Boolean.TRUE);
        user.setPasswordHash(passwordHashingService.hash("secret1"));
        when(dao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(user));
        UserAccountService userService = new UserAccountService(dao, tenantId -> {
        }, passwordHashingService);
        UserSessionService sessionService = new UserSessionService(userService);

        assertThatThrownBy(() -> sessionService.login("tenant-a", "alice", "wrong-password"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("invalid username or password");
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
        user.setEnabled(Boolean.TRUE);
        user.setPasswordHash(passwordHashingService.hash("secret1"));
        return user;
    }
}
