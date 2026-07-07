package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAccountWebControllerTest {
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldRevokeUserSessionsAfterPasswordChanged() {
        UserAccountService userAccountService = mock(UserAccountService.class);
        UserSessionService userSessionService = mock(UserSessionService.class);
        UserAccountWebController controller = new UserAccountWebController(provider(userSessionService));
        ReflectionTestUtils.setField(controller, "service", userAccountService);
        when(userAccountService.changePassword("user-1", "secret2")).thenReturn(1);

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            assertThat(controller.changePassword("user-1",
                    new UserAccountWebController.ChangePasswordRequest("secret2")).count()).isEqualTo(1);
        }

        verify(userSessionService).revokeUserSessions("user-1");
    }

    @Test
    void shouldRevokeUserSessionsAfterPasswordReset() {
        UserAccountService userAccountService = mock(UserAccountService.class);
        UserSessionService userSessionService = mock(UserSessionService.class);
        UserAccountWebController controller = new UserAccountWebController(provider(userSessionService));
        ReflectionTestUtils.setField(controller, "service", userAccountService);
        when(userAccountService.resetPassword("user-1")).thenReturn(
                new UserAccountService.PasswordResetResult(1, "temp-secret", Instant.parse("2026-07-08T00:00:00Z")));

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            UserAccountWebController.ResetPasswordResponse response = controller.resetPassword("user-1");

            assertThat(response.count()).isEqualTo(1);
            assertThat(response.temporaryPassword()).isEqualTo("temp-secret");
        }

        verify(userSessionService).revokeUserSessions("user-1");
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<UserSessionService> provider(UserSessionService userSessionService) {
        ObjectProvider<UserSessionService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(userSessionService);
        return provider;
    }
}
