package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
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

        verify(roleService, never()).hasActionPermission("system", "sales.contract", "query");
    }

    @Test
    void shouldAllowWhenRoleActionIsEnabled() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.hasActionPermission("user-1", "sales.contract", "query")).thenReturn(true);
        RoleActionExecutionPolicyService policy = new RoleActionExecutionPolicyService(roleService);

        policy.requireAuthorized(context(CurrentUser.tenantUser("user-1", "Alice", "tenant_a")));

        verify(roleService).hasActionPermission("user-1", "sales.contract", "query");
    }

    @Test
    void shouldRejectWhenNoRoleActionMatches() {
        RoleService roleService = mock(RoleService.class);
        RoleActionExecutionPolicyService policy = new RoleActionExecutionPolicyService(roleService);

        assertThatThrownBy(() -> policy.requireAuthorized(context(CurrentUser.tenantUser("user-1", "Alice", "tenant_a"))))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("sales.contract:query");
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

    private ActionExecutionContext context(CurrentUser user) {
        return ActionExecutionContext.ofActionCode(
                "sales.contract",
                "query",
                Set.of("contract-1"),
                Optional.of(user)
        );
    }
}
