package net.ximatai.muyun.spring.boot.iam;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.iam.role.AccountRoleGrant;
import net.ximatai.muyun.spring.iam.role.DataScopePolicy;
import net.ximatai.muyun.spring.iam.role.EmploymentRoleGrant;
import net.ximatai.muyun.spring.iam.role.GrantableAction;
import net.ximatai.muyun.spring.iam.role.ManagementScopeType;
import net.ximatai.muyun.spring.iam.role.RolePermissionAction;
import net.ximatai.muyun.spring.iam.role.RolePermissionMatrix;
import net.ximatai.muyun.spring.iam.role.RoleService;
import net.ximatai.muyun.spring.iam.role.TenantScopePolicy;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleWebControllerTest {
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldDeclareRoleActionRoutesWithJaxRsAnnotations() throws Exception {
        assertThat(RoleWebController.class.getAnnotation(Path.class).value()).isEqualTo("/iam.role");
        assertRoute(RoleWebController.class.getMethod("accountRoleGrants", String.class),
                GET.class, "/{roleId}/account-grants", "accountRoleGrants");
        assertRoute(RoleWebController.class.getMethod("grantAccountRole",
                        String.class, RoleWebController.AccountRoleGrantRequest.class),
                POST.class, "/{roleId}/account-grants", "accountRoleGrants");
        assertRoute(RoleWebController.class.getMethod("grantAction",
                        String.class, RoleWebController.GrantActionRequest.class),
                POST.class, "/grant/{roleId}", "rolePermissions");
        assertRoute(RoleWebController.class.getMethod("permissionMatrix",
                        String.class, RoleWebController.PermissionMatrixRequest.class),
                POST.class, "/permissionMatrix/{roleId}", "rolePermissions");
        assertRoute(RoleWebController.class.getMethod("menuMatrix", String.class, String.class),
                GET.class, "/menuMatrix/{roleId}/{schemeId}", "rolePermissions");
    }

    @Test
    void shouldExposeAccountAndEmploymentRoleGrantActions() {
        RoleService roleService = mock(RoleService.class);
        TestRoleWebController controller = new TestRoleWebController(roleService, mock(RoleGrantableActionResolver.class));
        AccountRoleGrant accountGrant = accountRoleGrant("grant-1", "role-1", "user-2",
                ManagementScopeType.TENANT, "tenant-a");
        EmploymentRoleGrant employmentGrant = employmentRoleGrant("grant-2", "role-2", "position-1");
        when(roleService.grantAccountRole("role-1", "user-2", ManagementScopeType.TENANT, "tenant-a"))
                .thenReturn("grant-1");
        when(roleService.accountRoleGrants("role-1")).thenReturn(List.of(accountGrant));
        when(roleService.deleteAccountRoleGrant("role-1", "grant-1")).thenReturn(1);
        when(roleService.grantEmploymentRole("role-2", "position-1")).thenReturn("grant-2");
        when(roleService.employmentRoleGrants("role-2")).thenReturn(List.of(employmentGrant));
        when(roleService.deleteEmploymentRoleGrant("role-2", "grant-2")).thenReturn(1);

        String accountGrantId;
        List<AccountRoleGrant> accountGrants;
        WebCountResponse accountDelete;
        String employmentGrantId;
        List<EmploymentRoleGrant> employmentGrants;
        WebCountResponse employmentDelete;
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            accountGrantId = controller.grantAccountRole("role-1",
                    new RoleWebController.AccountRoleGrantRequest("user-2", ManagementScopeType.TENANT, "tenant-a"));
            accountGrants = controller.accountRoleGrants("role-1");
            accountDelete = controller.deleteAccountRoleGrant("role-1", "grant-1");
            employmentGrantId = controller.grantEmploymentRole("role-2",
                    new RoleWebController.EmploymentRoleGrantRequest("position-1"));
            employmentGrants = controller.employmentRoleGrants("role-2");
            employmentDelete = controller.deleteEmploymentRoleGrant("role-2", "grant-2");
        }

        assertThat(accountGrantId).isEqualTo("grant-1");
        assertThat(accountGrants).singleElement().extracting(AccountRoleGrant::getUserId).isEqualTo("user-2");
        assertThat(accountDelete.count()).isEqualTo(1);
        assertThat(employmentGrantId).isEqualTo("grant-2");
        assertThat(employmentGrants).singleElement()
                .extracting(EmploymentRoleGrant::getEmployeePositionId).isEqualTo("position-1");
        assertThat(employmentDelete.count()).isEqualTo(1);
    }

    @Test
    void shouldExposeSingleAndBatchPermissionGrantActions() {
        RoleService roleService = mock(RoleService.class);
        TestRoleWebController controller = new TestRoleWebController(roleService, mock(RoleGrantableActionResolver.class));
        when(roleService.grantAction("role-1", "sales.contract", "query",
                DataScopePolicy.OWNER, TenantScopePolicy.CURRENT_TENANT, null, null, null)).thenReturn(1);
        when(roleService.revokeAction("role-1", "sales.contract", "query")).thenReturn(1);
        when(roleService.grantActions(any(), any())).thenReturn(2);
        when(roleService.revokeActions(any(), any())).thenReturn(1);

        WebCountResponse grant;
        WebCountResponse revoke;
        WebCountResponse batchGrant;
        WebCountResponse batchRevoke;
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            grant = controller.grantAction("role-1", new RoleWebController.GrantActionRequest(
                    "sales.contract", "query", DataScopePolicy.OWNER, TenantScopePolicy.CURRENT_TENANT,
                    null, null, null));
            revoke = controller.revokeAction("role-1",
                    new RoleWebController.RevokeActionRequest("sales.contract", "query"));
            batchGrant = controller.grantActions("role-1", new RoleWebController.GrantActionsRequest(List.of(
                    new RoleWebController.GrantActionRequest("sales.contract", "query", null, null, null, null, null),
                    new RoleWebController.GrantActionRequest("sales.order", "menu", null, null, null, null, null)
            )));
            batchRevoke = controller.revokeActions("role-1", new RoleWebController.RevokeActionsRequest(List.of(
                    new RoleWebController.RevokeActionRequest("sales.contract", "query")
            )));
        }

        assertThat(grant.count()).isEqualTo(1);
        assertThat(revoke.count()).isEqualTo(1);
        assertThat(batchGrant.count()).isEqualTo(2);
        assertThat(batchRevoke.count()).isEqualTo(1);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RoleService.ActionGrantCommand>> grantCaptor = ArgumentCaptor.forClass(List.class);
        verify(roleService).grantActions(org.mockito.ArgumentMatchers.eq("role-1"), grantCaptor.capture());
        assertThat(grantCaptor.getValue()).extracting(RoleService.ActionGrantCommand::moduleAlias)
                .containsExactly("sales.contract", "sales.order");
    }

    @Test
    void shouldExposePermissionMatrixFromModuleAliases() {
        RoleService roleService = mock(RoleService.class);
        RoleGrantableActionResolver resolver = mock(RoleGrantableActionResolver.class);
        TestRoleWebController controller = new TestRoleWebController(roleService, resolver);
        List<GrantableAction> grantableActions = List.of(
                new GrantableAction("sales.contract", "query", "view", "Query", true, true));
        when(resolver.resolve(List.of("sales.contract"))).thenReturn(grantableActions);
        when(roleService.permissionMatrix("role-1", grantableActions)).thenReturn(permissionMatrix("role-1"));

        RolePermissionMatrix matrix;
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            matrix = controller.permissionMatrix("role-1",
                    new RoleWebController.PermissionMatrixRequest(List.of("sales.contract")));
        }

        assertThat(matrix.roleId()).isEqualTo("role-1");
        assertThat(matrix.modules()).singleElement()
                .satisfies(module -> {
                    assertThat(module.moduleAlias()).isEqualTo("sales.contract");
                    assertThat(module.actions()).singleElement()
                            .satisfies(action -> {
                                assertThat(action.actionCode()).isEqualTo("query");
                                assertThat(action.permissionActionCode()).isEqualTo("view");
                                assertThat(action.granted()).isTrue();
                            });
                });
        verify(resolver).resolve(List.of("sales.contract"));
    }

    @Test
    void shouldExposeRoleMenuMatrixFromMenuTree() {
        RoleService roleService = mock(RoleService.class);
        RoleGrantableActionResolver resolver = mock(RoleGrantableActionResolver.class);
        MenuService menuService = mock(MenuService.class);
        TestRoleWebController controller = new TestRoleWebController(roleService, resolver, menuService);
        Menu group = menu("group-1", "scheme-1", null);
        Menu contract = menu("menu-1", "scheme-1", "sales.contract");
        Menu organization = menu("menu-2", "scheme-1", "iam.organization");
        Menu docs = menu("menu-3", "scheme-1", "platform.docs");
        docs.setExternalUrl("https://example.com/docs");
        when(menuService.rootMenus("scheme-1")).thenReturn(List.of(group));
        when(menuService.children("scheme-1", "group-1")).thenReturn(List.of(contract, organization, docs));
        when(menuService.children("scheme-1", "menu-1")).thenReturn(List.of());
        when(menuService.children("scheme-1", "menu-2")).thenReturn(List.of());
        when(menuService.children("scheme-1", "menu-3")).thenReturn(List.of());
        when(roleService.permissionMatrix(any(), any())).thenReturn(menuPermissionMatrix("role-1"));

        WebListResponse<RoleWebController.RoleMenuNode> response;
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            response = controller.menuMatrix("role-1", "scheme-1");
        }

        assertThat(response.records()).singleElement()
                .satisfies(root -> {
                    assertThat(root.menu().getId()).isEqualTo("group-1");
                    assertThat(root.children()).extracting(node -> node.menu().getId())
                            .containsExactly("menu-1", "menu-2", "menu-3");
                    assertThat(root.children()).allSatisfy(child -> assertThat(child.granted()).isTrue());
                });
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GrantableAction>> actionsCaptor = ArgumentCaptor.forClass(List.class);
        verify(roleService).permissionMatrix(org.mockito.ArgumentMatchers.eq("role-1"), actionsCaptor.capture());
        assertThat(actionsCaptor.getValue()).extracting(GrantableAction::moduleAlias)
                .containsExactly("sales.contract", "iam.organization", "platform.docs");
    }

    private void assertRoute(Method method, Class<?> httpMethod, String path, String actionCode) {
        assertThat(method.getAnnotation(httpMethod.asSubclass(Annotation.class))).isNotNull();
        assertThat(method.getAnnotation(Path.class).value()).isEqualTo(path);
        CustomActionEndpoint endpoint = method.getAnnotation(CustomActionEndpoint.class);
        assertThat(endpoint).isNotNull();
        assertThat(endpoint.value()).isEqualTo(actionCode);
        assertThat(endpoint.level()).isEqualTo(PlatformActionLevel.RECORD);
        assertThat(endpoint.recordIdPathVariable()).isEqualTo("roleId");
    }

    private AccountRoleGrant accountRoleGrant(String id,
                                              String roleId,
                                              String userId,
                                              ManagementScopeType scopeType,
                                              String scopeId) {
        AccountRoleGrant grant = new AccountRoleGrant();
        grant.setId(id);
        grant.setRoleId(roleId);
        grant.setUserId(userId);
        grant.setManagementScopeType(scopeType);
        grant.setManagementScopeId(scopeId);
        grant.setEnabled(true);
        return grant;
    }

    private EmploymentRoleGrant employmentRoleGrant(String id, String roleId, String employeePositionId) {
        EmploymentRoleGrant grant = new EmploymentRoleGrant();
        grant.setId(id);
        grant.setRoleId(roleId);
        grant.setEmployeePositionId(employeePositionId);
        grant.setEnabled(true);
        return grant;
    }

    private RolePermissionMatrix permissionMatrix(String roleId) {
        return new RolePermissionMatrix(
                roleId,
                List.of(new RolePermissionMatrix.Module(
                        "sales.contract",
                        List.of(new RolePermissionAction(
                                "sales.contract", "query", "view", "Query",
                                true, true, true, DataScopePolicy.OWNER,
                                TenantScopePolicy.CURRENT_TENANT, null, null, null))
                ))
        );
    }

    private RolePermissionMatrix menuPermissionMatrix(String roleId) {
        return new RolePermissionMatrix(
                roleId,
                List.of(
                        menuModule("sales.contract"),
                        menuModule("iam.organization"),
                        menuModule("platform.docs")
                )
        );
    }

    private RolePermissionMatrix.Module menuModule(String moduleAlias) {
        return new RolePermissionMatrix.Module(
                moduleAlias,
                List.of(new RolePermissionAction(
                        moduleAlias, "menu", "menu", "Menu",
                        true, false, true, DataScopePolicy.NONE,
                        TenantScopePolicy.CURRENT_TENANT, null, null, null))
        );
    }

    private Menu menu(String id, String schemeId, String moduleAlias) {
        Menu menu = new Menu();
        menu.setId(id);
        menu.setSchemeId(schemeId);
        menu.setModuleAlias(moduleAlias);
        menu.setTitle(id);
        menu.setEnabled(Boolean.TRUE);
        return menu;
    }

    private static final class TestRoleWebController extends RoleWebController {
        private TestRoleWebController(RoleService service, RoleGrantableActionResolver resolver) {
            super(resolver);
            this.service = service;
        }

        private TestRoleWebController(RoleService service,
                                      RoleGrantableActionResolver resolver,
                                      MenuService menuService) {
            super(resolver, () -> menuService);
            this.service = service;
        }
    }
}
