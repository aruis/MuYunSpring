package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.BusinessPrincipal;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.iam.employee.Employee;
import net.ximatai.muyun.spring.iam.employee.EmployeeAccountService;
import net.ximatai.muyun.spring.iam.employee.EmployeePosition;
import net.ximatai.muyun.spring.iam.employee.EmployeePositionService;
import net.ximatai.muyun.spring.iam.employee.EmployeeService;
import net.ximatai.muyun.spring.iam.user.UserAccount;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.spy;
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
        RoleService service = service(roleDao, mock(RoleGrantDao.class), mock(RoleActionDao.class));

        Role group = role("group-1", "Sales Group", RoleKind.GROUP);
        group.setMemberRoleIds(" r1, r1, r2 ");

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            service.insert(group);
        }

        assertThat(group.getRoleKind()).isEqualTo(RoleKind.GROUP);
        assertThat(group.getMemberRoleIds()).isEqualTo("r1,r2");
        assertThat(group.getGrantSubjectTypes()).isEqualTo(RoleGrantSubjectType.USER_ACCOUNT.getCode());
        assertThat(group.getEnabled()).isTrue();
        assertThat(group.getBuiltIn()).isFalse();
        assertThat(group.getSystemManaged()).isFalse();
    }

    @Test
    void shouldNormalizeGrantSubjectTypesAndDefaultPositionTemplateToEmployeePosition() {
        RoleDao roleDao = mock(RoleDao.class);
        when(roleDao.insert(any())).thenReturn("r1");
        RoleService service = service(roleDao, mock(RoleGrantDao.class), mock(RoleActionDao.class));

        Role role = role("r1", "Sales Manager", RoleKind.STANDARD);
        role.setGrantSubjectTypes(" employee, userAccount, employee ");

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            service.insert(role);
        }

        assertThat(role.getGrantSubjectTypes()).isEqualTo("employee,userAccount");

        Role positionTemplate = role("r2", "Position Template", RoleKind.POSITION_TEMPLATE);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            service.insert(positionTemplate);
        }

        assertThat(positionTemplate.getGrantSubjectTypes()).isEqualTo("employeePosition");
    }

    @Test
    void shouldRejectUserBindingWhenRoleDoesNotAllowUserAccountGrant() {
        RoleDao roleDao = mock(RoleDao.class);
        Role positionTemplate = role("template-1", "Position Template", RoleKind.POSITION_TEMPLATE);
        positionTemplate.setGrantSubjectTypes(RoleGrantSubjectType.EMPLOYEE_POSITION.getCode());
        when(roleDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(positionTemplate));
        RoleService service = service(roleDao, mock(RoleGrantDao.class), mock(RoleActionDao.class));

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThatThrownBy(() -> service.bindUser("template-1", "user-1"))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("cannot be granted to userAccount");
        }
    }

    @Test
    void shouldRejectUnsupportedGrantSubjectTypeAsPlatformException() {
        RoleService service = service(mock(RoleDao.class), mock(RoleGrantDao.class), mock(RoleActionDao.class));
        Role role = role("r1", "Role", RoleKind.STANDARD);
        role.setGrantSubjectTypes("unknownSubject");

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThatThrownBy(() -> service.insert(role))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("unsupported role grant subject type");
        }
    }

    @Test
    void shouldRejectNonStandardRoleInGroup() {
        RoleDao roleDao = mock(RoleDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(role("data-scope", "Wildcard Data Scope", RoleKind.WILDCARD_DATA_SCOPE)));
        RoleService service = service(roleDao, mock(RoleGrantDao.class), mock(RoleActionDao.class));
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
        RoleService service = service(roleDao, mock(RoleGrantDao.class), actionDao);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThat(service.grantAction("r1", "sales.contract", "query")).isEqualTo(1);
            assertThat(service.revokeAction("r1", "sales.contract", "query")).isEqualTo(1);
        }

        verify(actionDao).insert(argThat(action ->
                action.getId() != null
                        && "tenant_a".equals(action.getTenantId())
                        && "view".equals(action.getActionCode())
                        && action.getTenantScopePolicy() == TenantScopePolicy.CURRENT_TENANT
                        && Boolean.TRUE.equals(action.getEnabled())));
        verify(actionDao).updateById(argThat(action ->
                "tenant_a".equals(action.getTenantId())
                        && Boolean.FALSE.equals(action.getEnabled())));
    }

    @Test
    void shouldStoreTenantScopePolicyOnActionGrant() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleActionDao actionDao = mock(RoleActionDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(standardRole("r1")));
        when(actionDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of());
        when(actionDao.insert(any())).thenReturn("ra1");
        RoleService service = service(roleDao, mock(RoleGrantDao.class), actionDao);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThat(service.grantAction("r1", "sales.contract", "query",
                    DataScopePolicy.ALL, TenantScopePolicy.ALL_TENANTS)).isEqualTo(1);
        }

        verify(actionDao).insert(argThat(action ->
                "view".equals(action.getActionCode())
                        && action.getDataScopePolicy() == DataScopePolicy.ALL
                        && action.getTenantScopePolicy() == TenantScopePolicy.ALL_TENANTS
                        && Boolean.TRUE.equals(action.getEnabled())));
    }

    @Test
    void shouldRejectCustomDataScopeBeforeGrantIsSaved() {
        RoleDao roleDao = mock(RoleDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(standardRole("r1")));
        RoleActionDao actionDao = mock(RoleActionDao.class);
        RoleService service = service(roleDao, mock(RoleGrantDao.class), actionDao);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThatThrownBy(() -> service.grantAction(
                    "r1", "sales.contract", "query",
                    DataScopePolicy.CUSTOM, TenantScopePolicy.CURRENT_TENANT,
                    "authUserId = ${userId}", null, null))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("custom data scope policy is not supported yet");
        }

        verify(actionDao, org.mockito.Mockito.never()).insert(any());
        verify(actionDao, org.mockito.Mockito.never()).updateById(any());
    }

    @Test
    void shouldStoreMergedPermissionActionWhenGrantingInheritedPlatformActions() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleActionDao actionDao = mock(RoleActionDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(standardRole("r1")));
        when(actionDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of());
        when(actionDao.insert(any())).thenReturn("ra1");
        RoleService service = service(roleDao, mock(RoleGrantDao.class), actionDao);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            service.grantAction("r1", "sales.contract", "tree");
            service.grantAction("r1", "sales.contract", "reference");
            service.grantAction("r1", "sales.contract", "disable");
        }

        verify(actionDao, org.mockito.Mockito.times(2)).insert(argThat(action -> "view".equals(action.getActionCode())));
        verify(actionDao).insert(argThat(action -> "enable".equals(action.getActionCode())));
    }

    @Test
    void shouldStorePermissionActionCodeReturnedByGrantVerifier() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleActionDao actionDao = mock(RoleActionDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(standardRole("r1")));
        when(actionDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of());
        when(actionDao.insert(any())).thenReturn("ra1");
        RoleActionGrantVerifier verifier = (moduleAlias, actionCode) -> {
            assertThat(moduleAlias).isEqualTo("sales.contract");
            assertThat(actionCode).isEqualTo("exportData");
            return "create";
        };
        RoleService service = new RoleService(roleDao, mock(RoleGrantDao.class), actionDao,
                activeTenantVerifier(), verifier);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThat(service.grantAction("r1", "sales.contract", "exportData")).isEqualTo(1);
        }

        verify(actionDao).insert(argThat(action -> "create".equals(action.getActionCode())));
    }

    @Test
    void shouldRevokeActionByPermissionActionCodeResolvedFromGrantVerifier() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleActionDao actionDao = mock(RoleActionDao.class);
        when(actionDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(enabledAction("ra1", "r1", "sales.contract", "create")));
        when(actionDao.updateById(any())).thenReturn(1);
        RoleActionGrantVerifier verifier = (moduleAlias, actionCode) -> {
            assertThat(moduleAlias).isEqualTo("sales.contract");
            assertThat(actionCode).isEqualTo("exportData");
            return "create";
        };
        RoleService service = new RoleService(roleDao, mock(RoleGrantDao.class), actionDao,
                activeTenantVerifier(), verifier);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThat(service.revokeAction("r1", "sales.contract", "exportData")).isEqualTo(1);
        }

        verify(actionDao).updateById(argThat(action ->
                "create".equals(action.getActionCode())
                        && Boolean.FALSE.equals(action.getEnabled())));
    }

    @Test
    void shouldGrantWildcardDataScopeActionOnlyOnWildcardRole() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleActionDao actionDao = mock(RoleActionDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(role("scope-1", "Wildcard Scope", RoleKind.WILDCARD_DATA_SCOPE)));
        when(actionDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of());
        when(actionDao.insert(any())).thenReturn("ra1");
        RoleService service = service(roleDao, mock(RoleGrantDao.class), actionDao);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThat(service.grantWildcardDataScopeAction(
                    "scope-1", "query", DataScopePolicy.OWNER, TenantScopePolicy.CURRENT_TENANT)).isEqualTo(1);
        }

        verify(actionDao).insert(argThat(action ->
                RoleService.WILDCARD_DATA_SCOPE_MODULE_ALIAS.equals(action.getModuleAlias())
                        && "view".equals(action.getActionCode())
                        && action.getDataScopePolicy() == DataScopePolicy.OWNER
                        && Boolean.TRUE.equals(action.getEnabled())));
    }

    @Test
    void shouldRejectNestedWildcardPolicyOnWildcardDataScopeRole() {
        RoleDao roleDao = mock(RoleDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(role("scope-1", "Wildcard Scope", RoleKind.WILDCARD_DATA_SCOPE)));
        RoleService service = service(roleDao, mock(RoleGrantDao.class), mock(RoleActionDao.class));

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThatThrownBy(() -> service.grantWildcardDataScopeAction(
                    "scope-1", "query", DataScopePolicy.WILDCARD, TenantScopePolicy.CURRENT_TENANT))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("standard data scope");
        }
    }

    @Test
    void shouldRejectDepartmentPolicyOnWildcardDataScopeRole() {
        RoleDao roleDao = mock(RoleDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(role("scope-1", "Wildcard Scope", RoleKind.WILDCARD_DATA_SCOPE)));
        RoleService service = service(roleDao, mock(RoleGrantDao.class), mock(RoleActionDao.class));

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThatThrownBy(() -> service.grantWildcardDataScopeAction(
                    "scope-1", "query", DataScopePolicy.DEPARTMENT, TenantScopePolicy.CURRENT_TENANT))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("standard data scope");
        }
    }

    @Test
    void shouldRejectBusinessActionGrantOnWildcardDataScopeRole() {
        RoleDao roleDao = mock(RoleDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(role("scope-1", "Wildcard Scope", RoleKind.WILDCARD_DATA_SCOPE)));
        RoleService service = service(roleDao, mock(RoleGrantDao.class), mock(RoleActionDao.class));

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThatThrownBy(() -> service.grantAction(
                    "scope-1", "sales.contract", "query", DataScopePolicy.OWNER, TenantScopePolicy.CURRENT_TENANT))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("cannot be granted business action directly");
        }
    }

    @Test
    void shouldAllowCustomActionCodeForWildcardDataScopeTemplate() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleActionDao actionDao = mock(RoleActionDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(role("scope-1", "Wildcard Scope", RoleKind.WILDCARD_DATA_SCOPE)));
        when(actionDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of());
        when(actionDao.insert(any())).thenReturn("ra1");
        RoleService service = service(roleDao, mock(RoleGrantDao.class), actionDao);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThat(service.grantWildcardDataScopeAction(
                    "scope-1", "approve", DataScopePolicy.ORGANIZATION, TenantScopePolicy.CURRENT_TENANT)).isEqualTo(1);
        }

        verify(actionDao).insert(argThat(action ->
                RoleService.WILDCARD_DATA_SCOPE_MODULE_ALIAS.equals(action.getModuleAlias())
                        && "approve".equals(action.getActionCode())
                        && action.getDataScopePolicy() == DataScopePolicy.ORGANIZATION));
    }

    @Test
    void shouldAuthorizeThroughRoleGroupMembers() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleGrantDao roleGrantDao = mock(RoleGrantDao.class);
        RoleActionDao actionDao = mock(RoleActionDao.class);
        RoleGrant binding = roleGrant("group-1", "user-1");
        Role group = role("group-1", "Group", RoleKind.GROUP);
        group.setMemberRoleIds("r1");
        when(roleGrantDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(binding));
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(group))
                .thenReturn(List.of(standardRole("r1")));
        when(actionDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(enabledAction("ra1", "r1", "sales.contract", "view")));
        RoleService service = service(roleDao, roleGrantDao, actionDao);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThat(service.effectiveRoleIds("user-1")).containsExactly("group-1", "r1");
            assertThat(service.hasActionPermission("user-1", "sales.contract", "query")).isTrue();
        }
    }

    @Test
    void shouldResolveEffectiveWildcardDataScopeGrantFromBoundWildcardRole() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleGrantDao roleGrantDao = mock(RoleGrantDao.class);
        RoleActionDao actionDao = mock(RoleActionDao.class);
        when(roleGrantDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(roleGrant("scope-1", "user-1")));
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(role("scope-1", "Wildcard Scope", RoleKind.WILDCARD_DATA_SCOPE)))
                .thenReturn(List.of(role("scope-1", "Wildcard Scope", RoleKind.WILDCARD_DATA_SCOPE)));
        RoleAction grant = enabledAction("ra1", "scope-1", RoleService.WILDCARD_DATA_SCOPE_MODULE_ALIAS, "view");
        grant.setDataScopePolicy(DataScopePolicy.OWNER);
        when(actionDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(grant));
        RoleService service = service(roleDao, roleGrantDao, actionDao);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            RoleAction resolved = service.effectiveWildcardDataScopeGrant("user-1", "query");
            assertThat(resolved).isSameAs(grant);
        }
    }

    @Test
    void shouldRejectMoreThanOneDataScopeRoleForSameUser() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleGrantDao roleGrantDao = mock(RoleGrantDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(role("scope-2", "Wildcard Data Scope 2", RoleKind.WILDCARD_DATA_SCOPE)))
                .thenReturn(List.of(role("scope-1", "Wildcard Data Scope 1", RoleKind.WILDCARD_DATA_SCOPE)))
                .thenReturn(List.of(role("scope-2", "Wildcard Data Scope 2", RoleKind.WILDCARD_DATA_SCOPE)));
        when(roleGrantDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(roleGrant("scope-1", "user-1")));
        RoleService service = service(roleDao, roleGrantDao, mock(RoleActionDao.class));

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThatThrownBy(() -> service.bindUser("scope-2", "user-1"))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("at most one wildcard data scope role");
        }
    }

    @Test
    void shouldBindAndListRoleGrantsWithoutDuplicatingExistingBindings() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleGrantDao roleGrantDao = mock(RoleGrantDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(standardRole("r1")));
        when(roleGrantDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of())
                .thenReturn(List.of())
                .thenReturn(List.of())
                .thenReturn(List.of(roleGrant("r1", "user-2")))
                .thenReturn(List.of(roleGrant("r1", "user-1"), roleGrant("r1", "user-2")));
        when(roleGrantDao.insert(any())).thenReturn("binding-1", "binding-2");
        RoleService service = service(roleDao, roleGrantDao, mock(RoleActionDao.class));

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThat(service.bindUsers("r1", List.of("user-1", "user-2", "user-2"))).isEqualTo(1);
            assertThat(service.userIds("r1")).containsExactly("user-1", "user-2");
        }
    }

    @Test
    void shouldKeepGroupAndWildcardRolesGrantableToUserAccount() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleGrantDao roleGrantDao = mock(RoleGrantDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(role("group-1", "Group", RoleKind.GROUP)))
                .thenReturn(List.of(role("scope-1", "Wildcard Scope", RoleKind.WILDCARD_DATA_SCOPE)));
        when(roleGrantDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of());
        when(roleGrantDao.insert(any())).thenReturn("binding-1", "binding-2");
        RoleService service = service(roleDao, roleGrantDao, mock(RoleActionDao.class));

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThat(service.bindUser("group-1", "user-1")).isEqualTo("binding-1");
            assertThat(service.bindUser("scope-1", "user-1")).isEqualTo("binding-2");
        }
    }

    @Test
    void shouldRejectWildcardDataScopeRoleGrantedToEmployeeSubject() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleGrantDao roleGrantDao = mock(RoleGrantDao.class);
        EmployeeService employeeService = mock(EmployeeService.class);
        Role role = role("scope-1", "Wildcard Scope", RoleKind.WILDCARD_DATA_SCOPE);
        role.setGrantSubjectTypes(RoleGrantSubjectType.EMPLOYEE.getCode());
        when(roleDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(role));
        RoleService service = new RoleService(roleDao, roleGrantDao, mock(RoleActionDao.class),
                activeTenantVerifier(), RoleActionGrantVerifier.platformActionsOnly(),
                null, employeeService, null);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThatThrownBy(() -> service.grantRole("scope-1", RoleGrantSubjectType.EMPLOYEE, "employee-1"))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("wildcard data scope role can only be granted to user account");
        }

        verify(roleGrantDao, org.mockito.Mockito.never()).insert(any());
    }

    @Test
    void shouldGrantPositionTemplateRoleToEmployeePosition() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleGrantDao roleGrantDao = mock(RoleGrantDao.class);
        EmployeePositionService employeePositionService = mock(EmployeePositionService.class);
        Role role = role("template-1", "Position Template", RoleKind.POSITION_TEMPLATE);
        role.setGrantSubjectTypes(RoleGrantSubjectType.EMPLOYEE_POSITION.getCode());
        when(roleDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(role));
        when(roleGrantDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of());
        when(roleGrantDao.insert(any())).thenReturn("grant-1");
        when(employeePositionService.requireEnabled("position-rel-1", "employee position is not active: position-rel-1"))
                .thenReturn(new EmployeePosition());
        RoleService service = new RoleService(roleDao, roleGrantDao, mock(RoleActionDao.class),
                activeTenantVerifier(), RoleActionGrantVerifier.platformActionsOnly(),
                null, null, employeePositionService);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThat(service.grantRole(
                    "template-1", RoleGrantSubjectType.EMPLOYEE_POSITION, "position-rel-1")).isEqualTo("grant-1");
        }

        verify(roleGrantDao).insert(argThat(grant ->
                "template-1".equals(grant.getRoleId())
                        && grant.getSubjectType() == RoleGrantSubjectType.EMPLOYEE_POSITION
                        && "position-rel-1".equals(grant.getSubjectId())
                        && Boolean.TRUE.equals(grant.getEnabled())));
    }

    @Test
    void shouldGrantRoleToEmployeeSubject() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleGrantDao roleGrantDao = mock(RoleGrantDao.class);
        EmployeeService employeeService = mock(EmployeeService.class);
        Role role = standardRole("r1");
        role.setGrantSubjectTypes(RoleGrantSubjectType.EMPLOYEE.getCode());
        when(roleDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(role));
        when(roleGrantDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of());
        when(roleGrantDao.insert(any())).thenReturn("grant-1");
        when(employeeService.requireEnabled("employee-1", "employee is not active: employee-1"))
                .thenReturn(new Employee());
        RoleService service = new RoleService(roleDao, roleGrantDao, mock(RoleActionDao.class),
                activeTenantVerifier(), RoleActionGrantVerifier.platformActionsOnly(),
                null, employeeService, null);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThat(service.grantRole("r1", RoleGrantSubjectType.EMPLOYEE, "employee-1")).isEqualTo("grant-1");
        }

        verify(employeeService).requireEnabled("employee-1", "employee is not active: employee-1");
    }

    @Test
    void shouldValidateUserAccountSubjectWhenGrantingRole() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleGrantDao roleGrantDao = mock(RoleGrantDao.class);
        UserAccountService userAccountService = mock(UserAccountService.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(standardRole("r1")));
        when(roleGrantDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of());
        when(roleGrantDao.insert(any())).thenReturn("grant-1");
        when(userAccountService.requireEnabled("user-1", "user account is not active: user-1"))
                .thenReturn(new UserAccount());
        RoleService service = new RoleService(roleDao, roleGrantDao, mock(RoleActionDao.class),
                activeTenantVerifier(), RoleActionGrantVerifier.platformActionsOnly(),
                userAccountService, null, null);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThat(service.bindUser("r1", "user-1")).isEqualTo("grant-1");
        }

        verify(userAccountService).requireEnabled("user-1", "user account is not active: user-1");
    }

    @Test
    void shouldRejectGrantWhenSubjectIsInactive() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleGrantDao roleGrantDao = mock(RoleGrantDao.class);
        UserAccountService userAccountService = mock(UserAccountService.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(standardRole("r1")));
        when(userAccountService.requireEnabled("user-1", "user account is not active: user-1"))
                .thenThrow(new PlatformException("user account is not active: user-1"));
        RoleService service = new RoleService(roleDao, roleGrantDao, mock(RoleActionDao.class),
                activeTenantVerifier(), RoleActionGrantVerifier.platformActionsOnly(),
                userAccountService, null, null);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThatThrownBy(() -> service.bindUser("r1", "user-1"))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("user account is not active");
        }

        verify(roleGrantDao, org.mockito.Mockito.never()).insert(any());
    }

    @Test
    void shouldAggregateEffectiveRoleGrantsFromAccountEmployeeAndPositionSources() {
        EmployeeAccountService employeeAccountService = mock(EmployeeAccountService.class);
        EmployeeService employeeService = mock(EmployeeService.class);
        EmployeePositionService employeePositionService = mock(EmployeePositionService.class);
        when(employeeAccountService.employeeIdOfUser("user-1")).thenReturn("employee-1");
        when(employeeService.select("employee-1")).thenReturn(employee("employee-1", "org-main", "dept-main", true));
        when(employeePositionService.positions("employee-1"))
                .thenReturn(List.of(employeePosition("position-rel-1", "employee-1", "org-branch", "dept-branch", true)));
        RoleService service = spy(new RoleService(
                mock(RoleDao.class), mock(RoleGrantDao.class), mock(RoleActionDao.class), activeTenantVerifier(),
                RoleActionGrantVerifier.platformActionsOnly(), null, employeeService, employeePositionService,
                employeeAccountService));
        doReturn(List.of(roleGrant("account-role", RoleGrantSubjectType.USER_ACCOUNT, "user-1")))
                .when(service).subjectRoleGrants(RoleGrantSubjectType.USER_ACCOUNT, "user-1");
        doReturn(List.of(roleGrant("employee-role", RoleGrantSubjectType.EMPLOYEE, "employee-1")))
                .when(service).subjectRoleGrants(RoleGrantSubjectType.EMPLOYEE, "employee-1");
        doReturn(List.of(roleGrant("position-role", RoleGrantSubjectType.EMPLOYEE_POSITION, "position-rel-1")))
                .when(service).subjectRoleGrants(RoleGrantSubjectType.EMPLOYEE_POSITION, "position-rel-1");
        doReturn(standardRole("account-role")).when(service).select("account-role");
        doReturn(standardRole("employee-role")).when(service).select("employee-role");
        doReturn(standardRole("position-role")).when(service).select("position-role");

        List<EffectiveRoleGrant> grants = service.effectiveRoleGrants("user-1");

        assertThat(grants)
                .extracting(EffectiveRoleGrant::roleId)
                .containsExactly("account-role", "employee-role", "position-role");
        assertThat(grants.get(0))
                .extracting(EffectiveRoleGrant::sourceType, EffectiveRoleGrant::sourceId,
                        EffectiveRoleGrant::organizationId, EffectiveRoleGrant::departmentId,
                        EffectiveRoleGrant::employeePositionId)
                .containsExactly(RoleGrantSubjectType.USER_ACCOUNT, "user-1", null, null, null);
        assertThat(grants.get(1))
                .extracting(EffectiveRoleGrant::sourceType, EffectiveRoleGrant::sourceId,
                        EffectiveRoleGrant::organizationId, EffectiveRoleGrant::departmentId,
                        EffectiveRoleGrant::employeePositionId)
                .containsExactly(RoleGrantSubjectType.EMPLOYEE, "employee-1", "org-main", "dept-main", null);
        assertThat(grants.get(2))
                .extracting(EffectiveRoleGrant::sourceType, EffectiveRoleGrant::sourceId,
                        EffectiveRoleGrant::organizationId, EffectiveRoleGrant::departmentId,
                        EffectiveRoleGrant::employeePositionId)
                .containsExactly(RoleGrantSubjectType.EMPLOYEE_POSITION, "position-rel-1",
                        "org-branch", "dept-branch", "position-rel-1");
        assertThat(service.effectiveRoleIds("user-1"))
                .containsExactly("account-role", "employee-role", "position-role");
    }

    @Test
    void shouldAggregateEffectiveRoleGrantsFromExplicitBusinessPrincipalOnly() {
        EmployeeService employeeService = mock(EmployeeService.class);
        EmployeePositionService employeePositionService = mock(EmployeePositionService.class);
        when(employeeService.select("employee-1")).thenReturn(employee("employee-1", "org-main", "dept-main", true));
        when(employeePositionService.select("position-rel-1"))
                .thenReturn(employeePosition("position-rel-1", "employee-1", "org-branch", "dept-branch", true));
        RoleService service = spy(new RoleService(
                mock(RoleDao.class), mock(RoleGrantDao.class), mock(RoleActionDao.class), activeTenantVerifier(),
                RoleActionGrantVerifier.platformActionsOnly(), null, employeeService, employeePositionService,
                null));
        doReturn(List.of(roleGrant("employee-role", RoleGrantSubjectType.EMPLOYEE, "employee-1")))
                .when(service).subjectRoleGrants(RoleGrantSubjectType.EMPLOYEE, "employee-1");
        doReturn(List.of(roleGrant("position-role", RoleGrantSubjectType.EMPLOYEE_POSITION, "position-rel-1")))
                .when(service).subjectRoleGrants(RoleGrantSubjectType.EMPLOYEE_POSITION, "position-rel-1");
        doReturn(standardRole("employee-role")).when(service).select("employee-role");
        doReturn(standardRole("position-role")).when(service).select("position-role");

        List<EffectiveRoleGrant> grants = service.effectiveRoleGrants(
                BusinessPrincipal.employeePosition("employee-1", "org-forged", "dept-forged", "position-rel-1"));

        assertThat(grants).extracting(EffectiveRoleGrant::roleId)
                .containsExactly("employee-role", "position-role");
        assertThat(grants.get(0))
                .extracting(EffectiveRoleGrant::sourceType, EffectiveRoleGrant::sourceId,
                        EffectiveRoleGrant::organizationId, EffectiveRoleGrant::departmentId,
                        EffectiveRoleGrant::employeePositionId)
                .containsExactly(RoleGrantSubjectType.EMPLOYEE, "employee-1", "org-main", "dept-main", null);
        assertThat(grants.get(1))
                .extracting(EffectiveRoleGrant::sourceType, EffectiveRoleGrant::sourceId,
                        EffectiveRoleGrant::organizationId, EffectiveRoleGrant::departmentId,
                        EffectiveRoleGrant::employeePositionId)
                .containsExactly(RoleGrantSubjectType.EMPLOYEE_POSITION, "position-rel-1",
                        "org-branch", "dept-branch", "position-rel-1");
    }

    @Test
    void shouldResolveActionGrantsFromBusinessPrincipalContext() {
        RoleActionDao roleActionDao = mock(RoleActionDao.class);
        RoleAction action = enabledAction("action-1", "position-role", "sales.contract", "view");
        when(roleActionDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(action));
        EmployeePositionService employeePositionService = mock(EmployeePositionService.class);
        when(employeePositionService.select("position-rel-1"))
                .thenReturn(employeePosition("position-rel-1", "employee-1", "org-branch", "dept-branch", true));
        RoleService service = spy(new RoleService(
                mock(RoleDao.class), mock(RoleGrantDao.class), roleActionDao, activeTenantVerifier(),
                RoleActionGrantVerifier.platformActionsOnly(), null, null, employeePositionService,
                null));
        doReturn(List.of(roleGrant("position-role", RoleGrantSubjectType.EMPLOYEE_POSITION, "position-rel-1")))
                .when(service).subjectRoleGrants(RoleGrantSubjectType.EMPLOYEE_POSITION, "position-rel-1");
        doReturn(standardRole("position-role")).when(service).select("position-role");

        List<EffectiveRoleActionGrant> grants = service.effectiveActionGrantsWithContext(
                BusinessPrincipal.employeePosition("employee-1", null, null, "position-rel-1"),
                "sales.contract",
                "query");

        assertThat(grants).singleElement().satisfies(grant -> {
            assertThat(grant.actionGrant()).isSameAs(action);
            assertThat(grant.roleGrant().sourceType()).isEqualTo(RoleGrantSubjectType.EMPLOYEE_POSITION);
            assertThat(grant.roleGrant().organizationId()).isEqualTo("org-branch");
            assertThat(grant.roleGrant().departmentId()).isEqualTo("dept-branch");
        });
    }

    @Test
    void shouldIgnoreExplicitEmployeePrincipalWhenServerSideEmployeeFactIsMissing() {
        RoleService service = spy(new RoleService(
                mock(RoleDao.class), mock(RoleGrantDao.class), mock(RoleActionDao.class), activeTenantVerifier(),
                RoleActionGrantVerifier.platformActionsOnly(), null, null, null, null));
        doReturn(List.of(roleGrant("employee-role", RoleGrantSubjectType.EMPLOYEE, "employee-1")))
                .when(service).subjectRoleGrants(RoleGrantSubjectType.EMPLOYEE, "employee-1");

        List<EffectiveRoleGrant> grants = service.effectiveRoleGrants(
                BusinessPrincipal.employee("employee-1", "org-forged", "dept-forged"));

        assertThat(grants).isEmpty();
    }

    @Test
    void shouldIgnoreExplicitPositionPrincipalWhenServerSidePositionFactIsMissing() {
        RoleService service = spy(new RoleService(
                mock(RoleDao.class), mock(RoleGrantDao.class), mock(RoleActionDao.class), activeTenantVerifier(),
                RoleActionGrantVerifier.platformActionsOnly(), null, null, null, null));
        doReturn(List.of(roleGrant("position-role", RoleGrantSubjectType.EMPLOYEE_POSITION, "position-rel-1")))
                .when(service).subjectRoleGrants(RoleGrantSubjectType.EMPLOYEE_POSITION, "position-rel-1");

        List<EffectiveRoleGrant> grants = service.effectiveRoleGrants(
                BusinessPrincipal.employeePosition("employee-1", "org-forged", "dept-forged", "position-rel-1"));

        assertThat(grants).isEmpty();
    }

    @Test
    void shouldExpandGroupRoleMembersWithSameEffectiveGrantContext() {
        RoleService service = spy(service(mock(RoleDao.class), mock(RoleGrantDao.class), mock(RoleActionDao.class)));
        Role group = role("group-1", "Group", RoleKind.GROUP);
        group.setMemberRoleIds("member-1");
        doReturn(List.of(roleGrant("group-1", RoleGrantSubjectType.USER_ACCOUNT, "user-1")))
                .when(service).subjectRoleGrants(RoleGrantSubjectType.USER_ACCOUNT, "user-1");
        doReturn(group).when(service).select("group-1");
        doReturn(standardRole("member-1")).when(service).select("member-1");

        List<EffectiveRoleGrant> grants = service.effectiveRoleGrants("user-1");

        assertThat(grants).extracting(EffectiveRoleGrant::roleId)
                .containsExactly("group-1", "member-1");
        assertThat(grants).allSatisfy(grant -> {
            assertThat(grant.sourceType()).isEqualTo(RoleGrantSubjectType.USER_ACCOUNT);
            assertThat(grant.sourceId()).isEqualTo("user-1");
        });
    }

    @Test
    void shouldReturnEffectiveActionGrantsWithRoleGrantContext() {
        RoleActionDao roleActionDao = mock(RoleActionDao.class);
        RoleAction action = enabledAction("action-1", "position-role", "sales.contract", "view");
        when(roleActionDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(action));
        RoleService service = spy(service(mock(RoleDao.class), mock(RoleGrantDao.class), roleActionDao));
        doReturn(List.of(
                new EffectiveRoleGrant("position-role", RoleGrantSubjectType.EMPLOYEE_POSITION,
                        "position-1", "org-branch", "dept-branch", "position-1"),
                new EffectiveRoleGrant("position-role", RoleGrantSubjectType.EMPLOYEE_POSITION,
                        "position-2", "org-other", "dept-other", "position-2")
        )).when(service).effectiveRoleGrants("user-1");

        List<EffectiveRoleActionGrant> grants = service.effectiveActionGrantsWithContext(
                "user-1", "sales.contract", "query");

        assertThat(grants).hasSize(2);
        assertThat(grants).allSatisfy(grant -> assertThat(grant.actionGrant()).isSameAs(action));
        assertThat(grants)
                .extracting(grant -> grant.roleGrant().employeePositionId())
                .containsExactly("position-1", "position-2");
    }

    @Test
    void shouldKeepLegacyEffectiveActionGrantsDistinctWhenRoleHasMultipleContexts() {
        RoleActionDao roleActionDao = mock(RoleActionDao.class);
        RoleAction action = enabledAction("action-1", "position-role", "sales.contract", "view");
        when(roleActionDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(action));
        RoleService service = spy(service(mock(RoleDao.class), mock(RoleGrantDao.class), roleActionDao));
        doReturn(List.of(
                new EffectiveRoleGrant("position-role", RoleGrantSubjectType.EMPLOYEE_POSITION,
                        "position-1", "org-branch", "dept-branch", "position-1"),
                new EffectiveRoleGrant("position-role", RoleGrantSubjectType.EMPLOYEE_POSITION,
                        "position-2", "org-other", "dept-other", "position-2")
        )).when(service).effectiveRoleGrants("user-1");

        List<RoleAction> grants = service.effectiveActionGrants("user-1", "sales.contract", "query");

        assertThat(grants).containsExactly(action);
    }

    @Test
    void shouldIgnoreDisabledEmployeeAndPositionWhenAggregatingEffectiveRoleGrants() {
        EmployeeAccountService employeeAccountService = mock(EmployeeAccountService.class);
        EmployeeService employeeService = mock(EmployeeService.class);
        EmployeePositionService employeePositionService = mock(EmployeePositionService.class);
        when(employeeAccountService.employeeIdOfUser("user-1")).thenReturn("employee-1");
        when(employeeService.select("employee-1")).thenReturn(employee("employee-1", "org-main", "dept-main", false));
        RoleService service = spy(new RoleService(
                mock(RoleDao.class), mock(RoleGrantDao.class), mock(RoleActionDao.class), activeTenantVerifier(),
                RoleActionGrantVerifier.platformActionsOnly(), null, employeeService, employeePositionService,
                employeeAccountService));
        doReturn(List.of(roleGrant("account-role", RoleGrantSubjectType.USER_ACCOUNT, "user-1")))
                .when(service).subjectRoleGrants(RoleGrantSubjectType.USER_ACCOUNT, "user-1");
        doReturn(standardRole("account-role")).when(service).select("account-role");

        assertThat(service.effectiveRoleGrants("user-1"))
                .extracting(EffectiveRoleGrant::roleId)
                .containsExactly("account-role");
    }

    @Test
    void shouldIgnoreDisabledEmployeePositionWhenAggregatingEffectiveRoleGrants() {
        EmployeeAccountService employeeAccountService = mock(EmployeeAccountService.class);
        EmployeeService employeeService = mock(EmployeeService.class);
        EmployeePositionService employeePositionService = mock(EmployeePositionService.class);
        when(employeeAccountService.employeeIdOfUser("user-1")).thenReturn("employee-1");
        when(employeeService.select("employee-1")).thenReturn(employee("employee-1", "org-main", "dept-main", true));
        when(employeePositionService.positions("employee-1"))
                .thenReturn(List.of(employeePosition("position-rel-1", "employee-1", "org-branch", "dept-branch", false)));
        RoleService service = spy(new RoleService(
                mock(RoleDao.class), mock(RoleGrantDao.class), mock(RoleActionDao.class), activeTenantVerifier(),
                RoleActionGrantVerifier.platformActionsOnly(), null, employeeService, employeePositionService,
                employeeAccountService));
        doReturn(List.of()).when(service).subjectRoleGrants(RoleGrantSubjectType.USER_ACCOUNT, "user-1");
        doReturn(List.of()).when(service).subjectRoleGrants(RoleGrantSubjectType.EMPLOYEE, "employee-1");

        assertThat(service.effectiveRoleGrants("user-1")).isEmpty();
    }

    @Test
    void shouldUnbindRoleGrantsInBatch() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleGrantDao roleGrantDao = mock(RoleGrantDao.class);
        RoleGrant user1 = roleGrant("r1", "user-1");
        user1.setId("binding-1");
        when(roleDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(standardRole("r1")));
        when(roleGrantDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(user1))
                .thenReturn(List.of());
        when(roleGrantDao.deleteById("binding-1")).thenReturn(1);
        RoleService service = service(roleDao, roleGrantDao, mock(RoleActionDao.class));

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThat(service.unbindUsers("r1", List.of("user-1", "user-2"))).isEqualTo(1);
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
        RoleService service = new RoleService(roleDao, mock(RoleGrantDao.class), actionDao,
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
        RoleService service = service(mock(RoleDao.class), mock(RoleGrantDao.class), actionDao);

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
        RoleService service = service(roleDao, mock(RoleGrantDao.class), actionDao);

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
                                    RolePermissionAction::tenantScopePolicy,
                                    RolePermissionAction::dataAuth)
                            .containsExactly("view", "view", true, DataScopePolicy.OWNER,
                                    TenantScopePolicy.CURRENT_TENANT, true);
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
        RoleService service = service(roleDao, mock(RoleGrantDao.class), actionDao);

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
        RoleService service = service(roleDao, mock(RoleGrantDao.class), mock(RoleActionDao.class));

        assertThatThrownBy(() -> service.permissionMatrix("group-1", List.of(
                GrantableAction.ofPlatformDefaults("sales.contract", PlatformAction.QUERY)
        )))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("role group cannot be granted directly");
    }

    private RoleService service(RoleDao roleDao, RoleGrantDao roleGrantDao, RoleActionDao roleActionDao) {
        return new RoleService(roleDao, roleGrantDao, roleActionDao, activeTenantVerifier());
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

    private RoleGrant roleGrant(String roleId, String userId) {
        return roleGrant(roleId, RoleGrantSubjectType.USER_ACCOUNT, userId);
    }

    private RoleGrant roleGrant(String roleId, RoleGrantSubjectType subjectType, String subjectId) {
        RoleGrant binding = new RoleGrant();
        binding.setRoleId(roleId);
        binding.setSubjectType(subjectType);
        binding.setSubjectId(subjectId);
        binding.setEnabled(Boolean.TRUE);
        return binding;
    }

    private Employee employee(String id, String organizationId, String departmentId, boolean enabled) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setOrganizationId(organizationId);
        employee.setDepartmentId(departmentId);
        employee.setEnabled(enabled);
        return employee;
    }

    private EmployeePosition employeePosition(String id, String employeeId, String organizationId,
                                              String departmentId, boolean enabled) {
        EmployeePosition position = new EmployeePosition();
        position.setId(id);
        position.setEmployeeId(employeeId);
        position.setOrganizationId(organizationId);
        position.setDepartmentId(departmentId);
        position.setEnabled(enabled);
        return position;
    }

    private RoleAction enabledAction(String id, String roleId, String moduleAlias, String actionCode) {
        RoleAction action = new RoleAction();
        action.setId(id);
        action.setRoleId(roleId);
        action.setModuleAlias(moduleAlias);
        action.setActionCode(actionCode);
        action.setTenantId("tenant_a");
        action.setTenantScopePolicy(TenantScopePolicy.CURRENT_TENANT);
        action.setEnabled(Boolean.TRUE);
        return action;
    }

    private ActiveTenantVerifier activeTenantVerifier() {
        return mock(ActiveTenantVerifier.class);
    }
}
