package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.spring.common.identity.CurrentUser;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleMenuVisibilityPolicyServiceTest {
    @Test
    void shouldUseViewActionPermissionForModuleMenuVisibility() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.hasActionPermission("user-1", "crm.customer", "view")).thenReturn(true);
        RoleMenuVisibilityPolicyService service = new RoleMenuVisibilityPolicyService(roleService);

        assertThat(service.canViewModuleMenu(
                "crm.customer",
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a")))).isTrue();
        assertThat(service.canViewModuleMenu(
                "crm.contract",
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a")))).isFalse();
    }

    @Test
    void shouldKeepSystemUserVisibleAndAnonymousHidden() {
        RoleMenuVisibilityPolicyService service = new RoleMenuVisibilityPolicyService(mock(RoleService.class));

        assertThat(service.canViewModuleMenu(
                "crm.customer",
                Optional.of(CurrentUser.systemUser("system", "System")))).isTrue();
        assertThat(service.canViewModuleMenu("crm.customer", Optional.empty())).isFalse();
    }
}
