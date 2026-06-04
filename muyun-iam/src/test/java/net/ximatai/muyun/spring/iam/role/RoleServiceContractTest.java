package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleServiceContractTest {
    @Test
    void shouldDefaultRoleAsStandardAndNormalizeGroupMembers() {
        RoleDao roleDao = mock(RoleDao.class);
        when(roleDao.insert(any())).thenReturn("group-1");
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(standardRole("r1")))
                .thenReturn(List.of(standardRole("r2")));
        RoleService service = service(roleDao, mock(RoleUserDao.class), mock(RoleActionDao.class));

        Role group = role("group-1", "Sales Group", RoleKind.GROUP);
        group.setMemberRoleIds(" r1, r1, r2 ");

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            service.insert(group);
        }

        assertThat(group.getRoleKind()).isEqualTo(RoleKind.GROUP);
        assertThat(group.getTenantScopePolicy()).isEqualTo(TenantScopePolicy.CURRENT_TENANT);
        assertThat(group.getMemberRoleIds()).isEqualTo("r1,r2");
        assertThat(group.getEnabled()).isTrue();
    }

    @Test
    void shouldRejectNonStandardRoleInGroup() {
        RoleDao roleDao = mock(RoleDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(role("data-scope", "Wildcard Data Scope", RoleKind.WILDCARD_DATA_SCOPE)));
        RoleService service = service(roleDao, mock(RoleUserDao.class), mock(RoleActionDao.class));
        Role group = role("group-1", "Group", RoleKind.GROUP);
        group.setMemberRoleIds("data-scope");

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThatThrownBy(() -> service.insert(group))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("standard roles");
        }
    }

    @Test
    void shouldGrantAndRevokeRoleActionAsEnabledFact() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleActionDao actionDao = mock(RoleActionDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(standardRole("r1")));
        when(actionDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of())
                .thenReturn(List.of(enabledAction("ra1", "r1", "sales.contract", "query")));
        when(actionDao.insert(any())).thenAnswer(invocation -> {
            invocation.<RoleAction>getArgument(0).setId("ra1");
            return "ra1";
        });
        when(actionDao.updateById(any())).thenReturn(1);
        RoleService service = service(roleDao, mock(RoleUserDao.class), actionDao);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThat(service.grantAction("r1", "sales.contract", "query")).isEqualTo(1);
            assertThat(service.revokeAction("r1", "sales.contract", "query")).isEqualTo(1);
        }

        verify(actionDao).insert(argThat(action ->
                action.getId() != null
                        && "tenant_a".equals(action.getTenantId())
                        && "view".equals(action.getActionCode())
                        && Boolean.TRUE.equals(action.getEnabled())));
        verify(actionDao).updateById(argThat(action ->
                "tenant_a".equals(action.getTenantId())
                        && Boolean.FALSE.equals(action.getEnabled())));
    }

    @Test
    void shouldStoreMergedPermissionActionWhenGrantingInheritedPlatformActions() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleActionDao actionDao = mock(RoleActionDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(standardRole("r1")));
        when(actionDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of());
        when(actionDao.insert(any())).thenReturn("ra1");
        RoleService service = service(roleDao, mock(RoleUserDao.class), actionDao);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            service.grantAction("r1", "sales.contract", "tree");
            service.grantAction("r1", "sales.contract", "reference");
            service.grantAction("r1", "sales.contract", "disable");
        }

        verify(actionDao, org.mockito.Mockito.times(2)).insert(argThat(action -> "view".equals(action.getActionCode())));
        verify(actionDao).insert(argThat(action -> "enable".equals(action.getActionCode())));
    }

    @Test
    void shouldAuthorizeThroughRoleGroupMembers() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleUserDao roleUserDao = mock(RoleUserDao.class);
        RoleActionDao actionDao = mock(RoleActionDao.class);
        RoleUser binding = roleUser("group-1", "user-1");
        Role group = role("group-1", "Group", RoleKind.GROUP);
        group.setMemberRoleIds("r1");
        when(roleUserDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(binding));
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(group))
                .thenReturn(List.of(standardRole("r1")));
        when(actionDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(enabledAction("ra1", "r1", "sales.contract", "view")));
        RoleService service = service(roleDao, roleUserDao, actionDao);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThat(service.effectiveRoleIds("user-1")).containsExactly("group-1", "r1");
            assertThat(service.hasActionPermission("user-1", "sales.contract", "query")).isTrue();
        }
    }

    @Test
    void shouldResolveAllTenantScopeFromEffectiveRoles() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleUserDao roleUserDao = mock(RoleUserDao.class);
        Role allTenantRole = standardRole("r1");
        allTenantRole.setTenantScopePolicy(TenantScopePolicy.ALL_TENANTS);
        when(roleUserDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(roleUser("r1", "user-1")));
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(allTenantRole))
                .thenReturn(List.of(allTenantRole));
        RoleService service = service(roleDao, roleUserDao, mock(RoleActionDao.class));

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThat(service.hasAllTenantScope("user-1")).isTrue();
        }
    }

    @Test
    void shouldRejectMoreThanOneDataScopeRoleForSameUser() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleUserDao roleUserDao = mock(RoleUserDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(role("scope-2", "Wildcard Data Scope 2", RoleKind.WILDCARD_DATA_SCOPE)))
                .thenReturn(List.of(role("scope-1", "Wildcard Data Scope 1", RoleKind.WILDCARD_DATA_SCOPE)))
                .thenReturn(List.of(role("scope-2", "Wildcard Data Scope 2", RoleKind.WILDCARD_DATA_SCOPE)));
        when(roleUserDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(roleUser("scope-1", "user-1")));
        RoleService service = service(roleDao, roleUserDao, mock(RoleActionDao.class));

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThatThrownBy(() -> service.bindUser("scope-2", "user-1"))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("at most one wildcard data scope role");
        }
    }

    @Test
    void shouldUseGrantVerifierBeforeSavingAction() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleActionDao actionDao = mock(RoleActionDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(standardRole("r1")));
        RoleActionGrantVerifier verifier = (moduleAlias, actionCode) -> {
            throw new PlatformException("not grantable");
        };
        RoleService service = new RoleService(roleDao, mock(RoleUserDao.class), actionDao,
                activeTenantVerifier(), verifier);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThatThrownBy(() -> service.grantAction("r1", "sales.contract", "query"))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("not grantable");
        }
    }

    @Test
    void shouldReturnAlignedActionViewWithDisabledMissingActions() {
        RoleActionDao actionDao = mock(RoleActionDao.class);
        when(actionDao.query(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(List.of(enabledAction("ra1", "r1", "sales.contract", "query")));
        RoleService service = service(mock(RoleDao.class), mock(RoleUserDao.class), actionDao);

        List<RoleAction> actions = service.alignedActions(
                "r1",
                List.of("sales.contract"),
                List.of("query", "delete")
        );

        assertThat(actions).hasSize(2);
        assertThat(actions.get(0).getActionCode()).isEqualTo("query");
        assertThat(actions.get(0).getEnabled()).isTrue();
        assertThat(actions.get(1).getActionCode()).isEqualTo("delete");
        assertThat(actions.get(1).getEnabled()).isFalse();
    }

    @Test
    void shouldBuildRolePermissionMatrixFromGrantableActions() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleActionDao actionDao = mock(RoleActionDao.class);
        RoleAction viewGrant = enabledAction("ra1", "r1", "sales.contract", "view");
        viewGrant.setDataScopePolicy(DataScopePolicy.OWNER);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(standardRole("r1")));
        when(actionDao.query(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(List.of(viewGrant));
        RoleService service = service(roleDao, mock(RoleUserDao.class), actionDao);

        RolePermissionMatrix matrix = service.permissionMatrix("r1", List.of(
                GrantableAction.ofPlatformDefaults("sales.contract", PlatformAction.QUERY),
                GrantableAction.ofPlatformDefaults("sales.contract", PlatformAction.VIEW),
                GrantableAction.ofPlatformDefaults("sales.contract", PlatformAction.TREE),
                GrantableAction.ofPlatformDefaults("sales.contract", PlatformAction.REFERENCE),
                GrantableAction.ofPlatformDefaults("sales.contract", PlatformAction.DELETE),
                GrantableAction.ofPlatformDefaults("sales.contract", PlatformAction.DISABLE),
                GrantableAction.ofPlatformDefaults("sales.contract", PlatformAction.ENABLE)
        ));

        assertThat(matrix.roleId()).isEqualTo("r1");
        assertThat(matrix.modules()).singleElement()
                .satisfies(module -> {
                    assertThat(module.moduleAlias()).isEqualTo("sales.contract");
                    assertThat(module.actions()).hasSize(3);
                    assertThat(module.actions().get(0))
                            .extracting(RolePermissionAction::actionCode,
                                    RolePermissionAction::permissionActionCode,
                                    RolePermissionAction::granted,
                                    RolePermissionAction::dataScopePolicy,
                                    RolePermissionAction::dataAuth)
                            .containsExactly("view", "view", true, DataScopePolicy.OWNER, true);
                    assertThat(module.actions().get(1))
                            .extracting(RolePermissionAction::actionCode,
                                    RolePermissionAction::permissionActionCode,
                                    RolePermissionAction::granted,
                                    RolePermissionAction::dataScopePolicy)
                            .containsExactly("delete", "delete", false, DataScopePolicy.NONE);
                    assertThat(module.actions().get(2))
                            .extracting(RolePermissionAction::actionCode,
                                    RolePermissionAction::permissionActionCode,
                                    RolePermissionAction::granted)
                            .containsExactly("enable", "enable", false);
                });
    }

    @Test
    void shouldKeepPermissionMatrixShapeWithoutCrossModuleExpansion() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleActionDao actionDao = mock(RoleActionDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(standardRole("r1")));
        when(actionDao.query(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(List.of());
        RoleService service = service(roleDao, mock(RoleUserDao.class), actionDao);

        RolePermissionMatrix matrix = service.permissionMatrix("r1", List.of(
                GrantableAction.ofPlatformDefaults("sales.contract", PlatformAction.QUERY),
                GrantableAction.ofPlatformDefaults("iam.role", PlatformAction.UPDATE)
        ));

        assertThat(matrix.modules()).hasSize(2);
        assertThat(matrix.modules().get(0).moduleAlias()).isEqualTo("sales.contract");
        assertThat(matrix.modules().get(0).actions()).extracting(RolePermissionAction::actionCode)
                .containsExactly("query");
        assertThat(matrix.modules().get(1).moduleAlias()).isEqualTo("iam.role");
        assertThat(matrix.modules().get(1).actions()).extracting(RolePermissionAction::actionCode)
                .containsExactly("update");
    }

    @Test
    void shouldRejectPermissionMatrixForNonConfigurableRole() {
        RoleDao roleDao = mock(RoleDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(role("group-1", "Group", RoleKind.GROUP)));
        RoleService service = service(roleDao, mock(RoleUserDao.class), mock(RoleActionDao.class));

        assertThatThrownBy(() -> service.permissionMatrix("group-1", List.of(
                GrantableAction.ofPlatformDefaults("sales.contract", PlatformAction.QUERY)
        )))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("role group cannot be granted directly");
    }

    private RoleService service(RoleDao roleDao, RoleUserDao roleUserDao, RoleActionDao roleActionDao) {
        return new RoleService(roleDao, roleUserDao, roleActionDao, activeTenantVerifier());
    }

    private Role standardRole(String id) {
        return role(id, "Role " + id, RoleKind.STANDARD);
    }

    private Role role(String id, String title, RoleKind kind) {
        Role role = new Role();
        role.setId(id);
        role.setTitle(title);
        role.setRoleKind(kind);
        role.setEnabled(Boolean.TRUE);
        return role;
    }

    private RoleUser roleUser(String roleId, String userId) {
        RoleUser binding = new RoleUser();
        binding.setRoleId(roleId);
        binding.setUserId(userId);
        return binding;
    }

    private RoleAction enabledAction(String id, String roleId, String moduleAlias, String actionCode) {
        RoleAction action = new RoleAction();
        action.setId(id);
        action.setRoleId(roleId);
        action.setModuleAlias(moduleAlias);
        action.setActionCode(actionCode);
        action.setTenantId("tenant_a");
        action.setEnabled(Boolean.TRUE);
        return action;
    }

    private ActiveTenantVerifier activeTenantVerifier() {
        return mock(ActiveTenantVerifier.class);
    }
}
