package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.iam.role.DataScopePolicy;
import net.ximatai.muyun.spring.iam.role.GrantableAction;
import net.ximatai.muyun.spring.iam.role.RoleService;
import net.ximatai.muyun.spring.iam.role.TenantScopePolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BuiltInRolePermissionTemplateServiceTest {
    private final RoleService roleService = mock(RoleService.class);
    private final RoleGrantableActionResolver grantableActionResolver = mock(RoleGrantableActionResolver.class);
    private final BuiltInRolePermissionTemplateService service =
            new BuiltInRolePermissionTemplateService(roleService, grantableActionResolver);

    @Test
    void shouldApplyOrganizationAdminTemplateWithOrganizationAndChildrenDataScope() {
        when(grantableActionResolver.resolve(BuiltInRolePermissionTemplateService.ORGANIZATION_ADMIN_MODULE_ALIASES))
                .thenReturn(List.of(
                        GrantableAction.ofPlatformDefaults("iam.employee", PlatformAction.QUERY),
                        new GrantableAction("iam.employee", "employeeAccounts", "employeeAccounts",
                                "职员账号", true, true),
                        GrantableAction.ofPlatformDefaults("iam.employee", PlatformAction.MENU)
                ));

        service.applyOrganizationAdminTemplate("role-org-admin");

        assertThat(BuiltInRolePermissionTemplateService.ORGANIZATION_ADMIN_MODULE_ALIASES)
                .containsExactly("iam.organization", "iam.department", "iam.employee", "iam.user")
                .doesNotContain("iam.role", "iam.employee_account", "iam.tenant");
        verify(roleService).grantAction("role-org-admin", "iam.employee", PlatformAction.QUERY.code(),
                DataScopePolicy.ORGANIZATION_AND_CHILDREN, TenantScopePolicy.CURRENT_TENANT);
        verify(roleService).grantAction("role-org-admin", "iam.employee", PlatformAction.MENU.code(),
                DataScopePolicy.NONE, TenantScopePolicy.CURRENT_TENANT);
        verify(roleService, never()).grantAction("role-org-admin", "iam.employee", "employeeAccounts",
                DataScopePolicy.ORGANIZATION_AND_CHILDREN, TenantScopePolicy.CURRENT_TENANT);
    }
}
