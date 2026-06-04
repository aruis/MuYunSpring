package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.platform.ActionAccessMode;
import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleActionExecutionPolicyServiceTest {
    @Test
    void shouldAllowSystemUserWithoutRoleLookup() {
        RoleService roleService = mock(RoleService.class);
        RoleActionExecutionPolicyService policy = new RoleActionExecutionPolicyService(roleService);

        policy.requireAuthorized(context(CurrentUser.systemUser("system", "System")));

        verify(roleService, never()).hasActionPermission("system", "sales.contract", "view");
    }

    @Test
    void shouldAllowWhenRoleActionIsEnabled() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.hasActionPermission("user-1", "sales.contract", "view")).thenReturn(true);
        RoleActionExecutionPolicyService policy = new RoleActionExecutionPolicyService(roleService);

        policy.requireAuthorized(context(CurrentUser.tenantUser("user-1", "Alice", "tenant_a")));

        verify(roleService).hasActionPermission("user-1", "sales.contract", "view");
    }

    @Test
    void shouldRejectWhenNoRoleActionMatches() {
        RoleService roleService = mock(RoleService.class);
        RoleActionExecutionPolicyService policy = new RoleActionExecutionPolicyService(roleService);

        assertThatThrownBy(() -> policy.requireAuthorized(context(CurrentUser.tenantUser("user-1", "Alice", "tenant_a"))))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("sales.contract:view");
    }

    @Test
    void shouldRejectAnonymousAction() {
        RoleActionExecutionPolicyService policy = new RoleActionExecutionPolicyService(mock(RoleService.class));

        assertThatThrownBy(() -> policy.requireAuthorized(ActionExecutionContext.ofActionCode(
                "sales.contract",
                "query",
                Set.of(),
                Optional.empty()
        ))).isInstanceOf(PlatformException.class)
                .hasMessageContaining("current user");
    }

    @Test
    void shouldAllowAnonymousPolicyWithoutCurrentUser() {
        RoleService roleService = mock(RoleService.class);
        RoleActionExecutionPolicyService policy = new RoleActionExecutionPolicyService(roleService);

        policy.requireAuthorized(ActionExecutionContext.ofPolicy(
                "sales.contract",
                new ActionExecutionPolicy("publicSearch", PlatformActionLevel.LIST,
                        ActionAccessMode.ANONYMOUS_ALLOWED, false, false, ActionDefaultGrantPolicy.NONE, null),
                Set.of(),
                Optional.empty()
        ));

        verify(roleService, never()).hasActionPermission("user-1", "sales.contract", "publicSearch");
    }

    @Test
    void shouldAllowLoginOnlyPolicyWithoutRoleLookup() {
        RoleService roleService = mock(RoleService.class);
        RoleActionExecutionPolicyService policy = new RoleActionExecutionPolicyService(roleService);

        policy.requireAuthorized(ActionExecutionContext.ofPolicy(
                "sales.contract",
                new ActionExecutionPolicy("profile", PlatformActionLevel.RECORD,
                        ActionAccessMode.LOGIN_REQUIRED, false, false, ActionDefaultGrantPolicy.NONE, null),
                Set.of("contract-1"),
                Optional.of(CurrentUser.tenantUser("user-1", "Alice", "tenant_a"))
        ));

        verify(roleService, never()).hasActionPermission("user-1", "sales.contract", "profile");
    }

    @Test
    void shouldUseInheritedActionCodeForRoleLookup() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.hasActionPermission("user-1", "sales.contract", "view")).thenReturn(true);
        RoleActionExecutionPolicyService policy = new RoleActionExecutionPolicyService(roleService);

        policy.requireAuthorized(ActionExecutionContext.ofPolicy(
                "sales.contract",
                new ActionExecutionPolicy("export", PlatformActionLevel.LIST,
                        ActionAccessMode.AUTH_REQUIRED, true, false, ActionDefaultGrantPolicy.NONE, "view"),
                Set.of(),
                Optional.of(CurrentUser.tenantUser("user-1", "Alice", "tenant_a"))
        ));

        verify(roleService).hasActionPermission("user-1", "sales.contract", "view");
    }

    @Test
    void shouldAllowDefaultAuthenticatedGrantWithoutRoleLookup() {
        RoleService roleService = mock(RoleService.class);
        RoleActionExecutionPolicyService policy = new RoleActionExecutionPolicyService(roleService);

        policy.requireAuthorized(ActionExecutionContext.ofPolicy(
                "sales.contract",
                new ActionExecutionPolicy("follow", PlatformActionLevel.RECORD,
                        ActionAccessMode.AUTH_REQUIRED, true, true, ActionDefaultGrantPolicy.OWNER, null),
                Set.of("contract-1"),
                Optional.of(CurrentUser.tenantUser("user-1", "Alice", "tenant_a"))
        ));

        verify(roleService, never()).hasActionPermission("user-1", "sales.contract", "follow");
    }

    private ActionExecutionContext context(CurrentUser user) {
        return ActionExecutionContext.ofActionCode(
                "sales.contract",
                "query",
                Set.of("contract-1"),
                Optional.of(user)
        );
    }
}
