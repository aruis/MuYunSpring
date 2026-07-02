package net.ximatai.muyun.spring.boot.iam;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import net.ximatai.muyun.spring.boot.platform.PlatformMenu;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuGroups;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.common.di.ObjectProviders;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.iam.role.RoleService;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAccountWebControllerTest {
    private final UserSessionService userSessionService = mock(UserSessionService.class);
    private final RoleService roleService = mock(RoleService.class);
    private final UserAccountService userAccountService = mock(UserAccountService.class);
    private final UserAccountWebController controller = new UserAccountWebController(
            ObjectProviders.of(userSessionService),
            ObjectProviders.of(roleService)
    );

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldDeclareUserAccountRoutesAndActionMetadata() throws Exception {
        assertThat(UserAccountWebController.class.getAnnotation(Path.class).value()).isEqualTo("/iam.user");
        PlatformStaticModule module = UserAccountWebController.class.getAnnotation(PlatformStaticModule.class);
        assertThat(module.application()).isEqualTo("iam");
        assertThat(module.alias()).isEqualTo("iam.user");
        PlatformMenu menu = UserAccountWebController.class.getAnnotation(PlatformMenu.class);
        assertThat(menu.parent()).isEqualTo(PlatformMenuGroups.IDENTITY);

        assertActionRoute("changePassword",
                new Class<?>[]{String.class, UserAccountWebController.ChangePasswordRequest.class},
                "/changePassword/{id}", "changePassword", PlatformActionLevel.RECORD, true);
        assertActionRoute("selector",
                new Class<?>[]{UserAccountWebController.UserSelectorRequest.class},
                "/selector/query", "userSelector", PlatformActionLevel.LIST, true);
    }

    @Test
    void shouldRevokeUserSessionsAfterPasswordChanged() throws Exception {
        setService(controller, userAccountService);
        when(userAccountService.changePassword("user-1", "secret2")).thenReturn(1);

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            assertThat(controller.changePassword("user-1",
                    new UserAccountWebController.ChangePasswordRequest("secret2")).count()).isEqualTo(1);
        }

        verify(userSessionService).revokeUserSessions("user-1");
    }

    @Test
    void shouldNotRevokeUserSessionsWhenPasswordWasNotChanged() throws Exception {
        setService(controller, userAccountService);
        when(userAccountService.changePassword("user-1", "secret2")).thenReturn(0);

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            assertThat(controller.changePassword("user-1",
                    new UserAccountWebController.ChangePasswordRequest("secret2")).count()).isZero();
        }

        verify(userSessionService, never()).revokeUserSessions("user-1");
    }

    @Test
    void shouldAllowMissingSessionServiceForPasswordChange() throws Exception {
        UserAccountWebController noSessionController = new UserAccountWebController(ObjectProviders.of(null));
        setService(noSessionController, userAccountService);
        when(userAccountService.changePassword("user-1", "secret2")).thenReturn(1);

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            assertThat(noSessionController.changePassword("user-1",
                    new UserAccountWebController.ChangePasswordRequest("secret2")).count()).isEqualTo(1);
        }
    }

    private void assertActionRoute(String methodName,
                                   Class<?>[] parameterTypes,
                                   String path,
                                   String actionCode,
                                   PlatformActionLevel level,
                                   boolean dataAuth) throws Exception {
        Method method = UserAccountWebController.class.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(POST.class)).isNotNull();
        assertThat(method.getAnnotation(Path.class).value()).isEqualTo(path);
        CustomActionEndpoint endpoint = method.getAnnotation(CustomActionEndpoint.class);
        assertThat(endpoint.value()).isEqualTo(actionCode);
        assertThat(endpoint.level()).isEqualTo(level);
        assertThat(endpoint.dataAuth()).isEqualTo(dataAuth);
    }

    private void setService(UserAccountWebController target, UserAccountService service)
            throws ReflectiveOperationException {
        Field field = WebSupport.class.getDeclaredField("service");
        field.setAccessible(true);
        field.set(target, service);
    }
}
