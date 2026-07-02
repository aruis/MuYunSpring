package net.ximatai.muyun.spring.boot.iam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import net.ximatai.muyun.spring.common.exception.AuthenticationFailedException;
import net.ximatai.muyun.spring.common.exception.AuthenticationRequiredException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.iam.user.LoginResult;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginWebControllerTest {
    private final UserSessionService userSessionService = mock(UserSessionService.class);
    private final LoginWebController controller = new LoginWebController(userSessionService);

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
        TenantContext.clear();
    }

    @Test
    void shouldDeclareAuthRoutes() throws Exception {
        assertThat(LoginWebController.class.getAnnotation(Path.class).value()).isEqualTo("/iam.auth");

        assertRoute("login", new Class<?>[]{LoginWebController.LoginRequest.class}, POST.class, "/login");
        assertRoute("logout", new Class<?>[]{HttpServletRequest.class}, POST.class, "/logout");
        assertRoute("context", new Class<?>[]{}, GET.class, "/context");
    }

    @Test
    void shouldLoginWithRequestPayload() {
        CurrentUser currentUser = CurrentUser.tenantUser("user-1", "Alice", "tenant-a", "org-1");
        LoginResult result = LoginResult.bearer("token-1", Instant.parse("2026-01-01T00:00:00Z"), currentUser);
        when(userSessionService.login("tenant-a", "alice", "secret1")).thenReturn(result);

        LoginResult response = controller.login(new LoginWebController.LoginRequest(
                "tenant-a", "alice", "secret1"));

        assertThat(response).isSameAs(result);
        verify(userSessionService).login("tenant-a", "alice", "secret1");
    }

    @Test
    void shouldLogoutWithBearerToken() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer token-1");

        controller.logout(request);

        verify(userSessionService).logout("token-1");
    }

    @Test
    void shouldLogoutWithNullTokenWhenAuthorizationHeaderIsMissingOrUnsupported() {
        HttpServletRequest missing = mock(HttpServletRequest.class);
        HttpServletRequest unsupported = mock(HttpServletRequest.class);
        when(unsupported.getHeader("Authorization")).thenReturn("Basic credential");

        controller.logout(missing);
        controller.logout(unsupported);

        verify(userSessionService, org.mockito.Mockito.times(2)).logout(null);
    }

    @Test
    void shouldExposeCurrentUserContext() {
        CurrentUser currentUser = CurrentUser.tenantUser("user-1", "Alice", "tenant-a", "org-1");

        CurrentUser response;
        try (CurrentUserContext.Scope ignored = CurrentUserContext.use(currentUser)) {
            response = controller.context();
        }

        assertThat(response).isSameAs(currentUser);
    }

    @Test
    void shouldRequireCurrentUserContext() {
        assertThatThrownBy(controller::context)
                .isInstanceOf(AuthenticationRequiredException.class)
                .hasMessage("current user context is not available");
    }

    @Test
    void shouldPropagateAuthenticationFailuresWithoutLeakingCauseMessage() {
        when(userSessionService.login(anyString(), anyString(), anyString()))
                .thenThrow(new AuthenticationFailedException("invalid username or password",
                        new RuntimeException("Tenant is not active: tenant-a")));

        assertThatThrownBy(() -> controller.login(new LoginWebController.LoginRequest(
                "tenant-a", "alice", "secret1")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("invalid username or password")
                .hasRootCauseMessage("Tenant is not active: tenant-a");
    }

    @Test
    void shouldPropagateMalformedLoginRequestAsValidationFailure() {
        when(userSessionService.login(null, "alice", "secret1"))
                .thenThrow(new IllegalArgumentException("tenantId must not be null"));

        assertThatThrownBy(() -> controller.login(new LoginWebController.LoginRequest(
                null, "alice", "secret1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tenantId must not be null");
    }

    private void assertRoute(String methodName,
                             Class<?>[] parameterTypes,
                             Class<?> httpMethod,
                             String path) throws Exception {
        Method method = LoginWebController.class.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(httpMethod.asSubclass(java.lang.annotation.Annotation.class))).isNotNull();
        assertThat(method.getAnnotation(Path.class).value()).isEqualTo(path);
    }
}
