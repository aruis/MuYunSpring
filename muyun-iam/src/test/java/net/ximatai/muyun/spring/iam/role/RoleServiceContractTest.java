package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaSqlCompiler;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.form.FormControlType;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.BusinessPrincipal;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.option.OptionBinding;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleServiceContractTest {
    @AfterEach
    void tearDown() {
        TenantContext.clear();
        CurrentUserContext.clear();
    }

    @Test
    void shouldExposeAssignmentAndRoleKindEnumBindings() {
        RoleService service = service(mock(RoleDao.class), mock(AccountRoleGrantDao.class),
                mock(EmploymentRoleGrantDao.class), mock(RoleActionDao.class));

        assertThat(service.querySchema().fields()).anySatisfy(field -> {
            assertThat(field.name()).isEqualTo("assignmentType");
            assertThat(field.optionBinding()).isEqualTo(OptionBinding.enumType(RoleAssignmentType.class));
            assertThat(field.optionTitleField()).isEqualTo("assignmentTypeTitle");
        });
        assertThat(service.formSchema().fields()).anySatisfy(field -> {
            assertThat(field.name()).isEqualTo("roleKind");
            assertThat(field.optionBinding()).isEqualTo(OptionBinding.enumType(RoleKind.class));
            assertThat(field.controlType()).isEqualTo(FormControlType.SELECT);
            assertThat(field.optionTitleField()).isEqualTo("roleKindTitle");
        });
    }

    @Test
    void shouldDefaultRoleAsEmploymentStandardAndNormalizeGroupMembers() {
        RoleDao roleDao = mock(RoleDao.class);
        when(roleDao.insert(any())).thenReturn("group-1");
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(employmentRole("r1", RoleKind.STANDARD)))
                .thenReturn(List.of(employmentRole("r2", RoleKind.DATA_GRANT)));
        RoleService service = service(roleDao, mock(AccountRoleGrantDao.class),
                mock(EmploymentRoleGrantDao.class), mock(RoleActionDao.class));

        Role role = role("r0", "Standard", null, null);
        Role group = role("group-1", "Sales Group", RoleAssignmentType.ACCOUNT, RoleKind.GROUP);
        group.setMemberRoleIds(" r1, r1, r2 ");

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            service.normalizeBeforeMutation(role);
            service.insert(group);
        }

        assertThat(role.getAssignmentType()).isEqualTo(RoleAssignmentType.EMPLOYMENT);
        assertThat(role.getRoleKind()).isEqualTo(RoleKind.STANDARD);
        assertThat(group.getAssignmentType()).isEqualTo(RoleAssignmentType.EMPLOYMENT);
        assertThat(group.getMemberRoleIds()).isEqualTo("r1,r2");
        assertThat(group.getEnabled()).isTrue();
        assertThat(group.getBuiltIn()).isFalse();
        assertThat(group.getSystemManaged()).isFalse();
    }

    @Test
    void shouldRejectAccountOrNestedGroupRoleInGroup() {
        RoleDao roleDao = mock(RoleDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(accountRole("account-role", RoleKind.STANDARD)));
        RoleService service = service(roleDao, mock(AccountRoleGrantDao.class),
                mock(EmploymentRoleGrantDao.class), mock(RoleActionDao.class));
        Role group = employmentRole("group-1", RoleKind.GROUP);
        group.setMemberRoleIds("account-role");

        assertThatThrownBy(() -> service.normalizeBeforeMutation(group))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("role group can only contain employment roles");
    }

    @Test
    void shouldRejectSystemManagedRoleMutationWithoutSystemUser() {
        RoleDao roleDao = mock(RoleDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(systemManagedRole("managed-1")));
        RoleService service = service(roleDao, mock(AccountRoleGrantDao.class),
                mock(EmploymentRoleGrantDao.class), mock(RoleActionDao.class));

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            Role created = employmentRole("managed-2", RoleKind.STANDARD);
            created.setSystemManaged(Boolean.TRUE);
            assertThatThrownBy(() -> service.insert(created))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("system managed role");
            assertThatThrownBy(() -> service.update(employmentRole("managed-1", RoleKind.STANDARD)))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("system managed role");
            assertThatThrownBy(() -> service.grantAccountRole(
                    "managed-1", "user-1", ManagementScopeType.TENANT, "tenant_a"))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("system managed role");
        }
    }

    @Test
    void shouldAllowSystemUserToMaintainSystemManagedRoleInTenantContext() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleActionDao actionDao = mock(RoleActionDao.class);
        when(roleDao.insert(any())).thenReturn("managed-1");
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(systemManagedRole("managed-1")));
        when(actionDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of());
        when(actionDao.insert(any())).thenReturn("ra1");
        RoleService service = service(roleDao, mock(AccountRoleGrantDao.class),
                mock(EmploymentRoleGrantDao.class), actionDao);

        try (TenantContext.Scope ignoredTenant = TenantContext.use("tenant_a");
             CurrentUserContext.Scope ignoredUser = CurrentUserContext.use(
                     CurrentUser.systemUser("bootstrap", "Bootstrap"))) {
            Role role = systemManagedRole("managed-1");
            assertThat(service.insert(role)).isEqualTo("managed-1");
            assertThat(service.grantAction("managed-1", "sales.contract", "query")).isEqualTo(1);
        }

        verify(roleDao).insert(argThat(role ->
                Boolean.TRUE.equals(role.getSystemManaged()) && "tenant_a".equals(role.getTenantId())));
        verify(actionDao).insert(argThat(action ->
                "managed-1".equals(action.getRoleId()) && "tenant_a".equals(action.getTenantId())));
    }

    @Test
    void shouldGrantAndListAccountRolesWithoutDataScope() {
        RoleDao roleDao = mock(RoleDao.class);
        AccountRoleGrantDao accountGrantDao = mock(AccountRoleGrantDao.class);
        UserAccountService userAccountService = mock(UserAccountService.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(accountRole("r1", RoleKind.STANDARD)));
        when(accountGrantDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of())
                .thenReturn(List.of(accountGrant("r1", "user-1", ManagementScopeType.TENANT, "tenant_a")));
        when(accountGrantDao.insert(any())).thenReturn("grant-1");
        when(userAccountService.requireEnabled("user-1", "user account is not active: user-1"))
                .thenReturn(new UserAccount());
        RoleService service = new RoleService(roleDao, accountGrantDao, mock(EmploymentRoleGrantDao.class),
                mock(RoleActionDao.class), activeTenantVerifier(), RoleActionGrantVerifier.platformActionsOnly(),
                userAccountService, null, null, null);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThat(service.grantAccountRole("r1", "user-1", ManagementScopeType.TENANT, "tenant_a"))
                    .isEqualTo("grant-1");
            assertThat(service.userIds("r1")).containsExactly("user-1");
        }

        verify(accountGrantDao).insert(argThat(grant ->
                "r1".equals(grant.getRoleId())
                        && "user-1".equals(grant.getUserId())
                        && grant.getManagementScopeType() == ManagementScopeType.TENANT
                        && "tenant_a".equals(grant.getManagementScopeId())
                        && Boolean.TRUE.equals(grant.getEnabled())));
    }

    @Test
    void shouldRejectDataScopeOnAccountRoleAction() {
        RoleDao roleDao = mock(RoleDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(accountRole("r1", RoleKind.STANDARD)));
        RoleService service = service(roleDao, mock(AccountRoleGrantDao.class),
                mock(EmploymentRoleGrantDao.class), mock(RoleActionDao.class));

        assertThatThrownBy(() -> service.grantAction(
                "r1", "sales.contract", "query", DataScopePolicy.OWNER, TenantScopePolicy.CURRENT_TENANT))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("account role action cannot configure data scope");
    }

    @Test
    void shouldGrantEmploymentRoleToEmployeePosition() {
        RoleDao roleDao = mock(RoleDao.class);
        EmploymentRoleGrantDao employmentGrantDao = mock(EmploymentRoleGrantDao.class);
        EmployeePositionService employeePositionService = mock(EmployeePositionService.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(employmentRole("r1", RoleKind.STANDARD)));
        when(employmentGrantDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of());
        when(employmentGrantDao.insert(any())).thenReturn("grant-1");
        when(employeePositionService.requireEnabled("position-1", "employee position is not active: position-1"))
                .thenReturn(new EmployeePosition());
        RoleService service = new RoleService(roleDao, mock(AccountRoleGrantDao.class), employmentGrantDao,
                mock(RoleActionDao.class), activeTenantVerifier(), RoleActionGrantVerifier.platformActionsOnly(),
                null, null, employeePositionService, null);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThat(service.grantEmploymentRole("r1", "position-1")).isEqualTo("grant-1");
        }

        verify(employmentGrantDao).insert(argThat(grant ->
                "r1".equals(grant.getRoleId())
                        && "position-1".equals(grant.getEmployeePositionId())
                        && Boolean.TRUE.equals(grant.getEnabled())));
    }

    @Test
    void shouldRejectAccountRoleGrantedToEmploymentAndEmploymentRoleGrantedToAccount() {
        RoleDao roleDao = mock(RoleDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(accountRole("account-role", RoleKind.STANDARD)))
                .thenReturn(List.of(employmentRole("employment-role", RoleKind.STANDARD)));
        RoleService service = service(roleDao, mock(AccountRoleGrantDao.class),
                mock(EmploymentRoleGrantDao.class), mock(RoleActionDao.class));

        assertThatThrownBy(() -> service.grantEmploymentRole("account-role", "position-1"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("role is not employment role");
        assertThatThrownBy(() -> service.grantAccountRole(
                "employment-role", "user-1", ManagementScopeType.TENANT, "tenant_a"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("role is not account role");
    }

    @Test
    void shouldRequireConcreteDataScopeOnDataGrantRole() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleActionDao actionDao = mock(RoleActionDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(employmentRole("data-role", RoleKind.DATA_GRANT)))
                .thenReturn(List.of(employmentRole("data-role", RoleKind.DATA_GRANT)));
        when(actionDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of());
        when(actionDao.insert(any())).thenReturn("ra1");
        RoleService service = service(roleDao, mock(AccountRoleGrantDao.class),
                mock(EmploymentRoleGrantDao.class), actionDao);

        assertThatThrownBy(() -> service.grantAction("data-role", "sales.contract", "query"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("data grant role must configure concrete data scope");
        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThat(service.grantAction(
                    "data-role", "sales.contract", "query", DataScopePolicy.ORGANIZATION,
                    TenantScopePolicy.CURRENT_TENANT)).isEqualTo(1);
        }
    }

    @Test
    void shouldRejectMoreThanOneDataGrantRoleForSameEmployment() {
        RoleDao roleDao = mock(RoleDao.class);
        EmploymentRoleGrantDao employmentGrantDao = mock(EmploymentRoleGrantDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(employmentRole("data-2", RoleKind.DATA_GRANT)))
                .thenReturn(List.of(employmentRole("data-1", RoleKind.DATA_GRANT)))
                .thenReturn(List.of(employmentRole("data-2", RoleKind.DATA_GRANT)));
        when(employmentGrantDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(employmentGrant("data-1", "position-1")));
        RoleService service = service(roleDao, mock(AccountRoleGrantDao.class),
                employmentGrantDao, mock(RoleActionDao.class));

        assertThatThrownBy(() -> service.grantEmploymentRole("data-2", "position-1"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("employment can have at most one data grant role");
    }

    @Test
    void shouldResolveInheritedDataGrantActionThroughEmploymentRoleGroupMember() {
        RoleDao roleDao = mock(RoleDao.class);
        EmploymentRoleGrantDao employmentGrantDao = mock(EmploymentRoleGrantDao.class);
        RoleActionDao actionDao = mock(RoleActionDao.class);
        Role group = employmentRole("group-1", RoleKind.GROUP);
        group.setMemberRoleIds("data-role");
        RoleAction grant = enabledAction("ra1", "data-role", "sales.contract", "view");
        grant.setDataScopePolicy(DataScopePolicy.ORGANIZATION);
        when(employmentGrantDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(employmentGrant("group-1", "position-1")));
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(group))
                .thenReturn(List.of(employmentRole("data-role", RoleKind.DATA_GRANT)));
        when(actionDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(grant));
        RoleService service = service(roleDao, mock(AccountRoleGrantDao.class), employmentGrantDao, actionDao);

        RoleAction resolved = service.inheritedDataGrantAction(
                EffectiveRoleGrant.employment("standard-role", "position-1", "org-1", "dept-1"),
                "sales.contract", "query");

        assertThat(resolved).isSameAs(grant);
    }

    @Test
    void shouldResolveInheritedDataGrantActionByModuleAlias() {
        RoleDao roleDao = mock(RoleDao.class);
        EmploymentRoleGrantDao employmentGrantDao = mock(EmploymentRoleGrantDao.class);
        RoleActionDao actionDao = mock(RoleActionDao.class);
        RoleAction orderGrant = enabledAction("ra2", "data-role", "sales.order", "view");
        orderGrant.setDataScopePolicy(DataScopePolicy.DEPARTMENT);
        when(employmentGrantDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(employmentGrant("data-role", "position-1")));
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(employmentRole("data-role", RoleKind.DATA_GRANT)));
        when(actionDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(orderGrant));
        RoleService service = service(roleDao, mock(AccountRoleGrantDao.class), employmentGrantDao, actionDao);

        RoleAction resolved = service.inheritedDataGrantAction(
                EffectiveRoleGrant.employment("standard-role", "position-1", "org-1", "dept-1"),
                "sales.order", "query");

        assertThat(resolved).isSameAs(orderGrant);
        verify(actionDao).query(argThat(criteria -> {
                    var compiled = new CriteriaSqlCompiler()
                            .compile(criteria, field -> field, DBInfo.Type.POSTGRESQL);
                    return compiled.getSql().contains("\"moduleAlias\" =")
                            && compiled.getParams().containsValue("sales.order");
                }),
                any(PageRequest.class));
    }

    @Test
    void shouldRejectRoleGroupUpdateWhenExistingEmploymentWouldHaveTwoDataGrantRoles() {
        EmploymentRoleGrantDao employmentGrantDao = mock(EmploymentRoleGrantDao.class);
        when(employmentGrantDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(employmentGrant("group-1", "position-1")))
                .thenReturn(List.of(
                        employmentGrant("group-1", "position-1"),
                        employmentGrant("data-1", "position-1")
                ));
        RoleService service = spy(service(mock(RoleDao.class), mock(AccountRoleGrantDao.class),
                employmentGrantDao, mock(RoleActionDao.class)));
        doReturn(employmentRole("data-1", RoleKind.DATA_GRANT)).when(service).select("data-1");
        doReturn(employmentRole("data-2", RoleKind.DATA_GRANT)).when(service).select("data-2");
        Role group = employmentRole("group-1", RoleKind.GROUP);
        group.setMemberRoleIds("data-2");

        assertThatThrownBy(() -> service.normalizeBeforeMutation(group))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("employment can have at most one data grant role");
    }

    @Test
    void shouldProtectSystemManagedRoleGrantsWhenDeletingByGrantId() {
        RoleDao roleDao = mock(RoleDao.class);
        AccountRoleGrantDao accountGrantDao = mock(AccountRoleGrantDao.class);
        EmploymentRoleGrantDao employmentGrantDao = mock(EmploymentRoleGrantDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(systemManagedRole("managed-1")))
                .thenReturn(List.of(systemManagedRole("managed-1")));
        RoleService service = service(roleDao, accountGrantDao, employmentGrantDao, mock(RoleActionDao.class));

        assertThatThrownBy(() -> service.deleteAccountRoleGrant("managed-1", "grant-1"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("system managed role");
        assertThatThrownBy(() -> service.deleteEmploymentRoleGrant("managed-1", "grant-1"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("system managed role");
        verify(accountGrantDao, never()).deleteById(any());
        verify(employmentGrantDao, never()).deleteById(any());
    }

    @Test
    void shouldGrantAndRevokeRoleActionAsEnabledFact() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleActionDao actionDao = mock(RoleActionDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(employmentRole("r1", RoleKind.STANDARD)));
        when(actionDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of())
                .thenReturn(List.of(enabledAction("ra1", "r1", "sales.contract", "view")));
        when(actionDao.insert(any())).thenAnswer(invocation -> {
            invocation.<RoleAction>getArgument(0).setId("ra1");
            return "ra1";
        });
        when(actionDao.updateById(any())).thenReturn(1);
        RoleService service = service(roleDao, mock(AccountRoleGrantDao.class),
                mock(EmploymentRoleGrantDao.class), actionDao);

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
    void shouldStorePermissionActionCodeReturnedByGrantVerifier() {
        RoleDao roleDao = mock(RoleDao.class);
        RoleActionDao actionDao = mock(RoleActionDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(employmentRole("r1", RoleKind.STANDARD)));
        when(actionDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of());
        when(actionDao.insert(any())).thenReturn("ra1");
        RoleActionGrantVerifier verifier = (moduleAlias, actionCode) -> {
            assertThat(moduleAlias).isEqualTo("sales.contract");
            assertThat(actionCode).isEqualTo("exportData");
            return "create";
        };
        RoleService service = new RoleService(roleDao, mock(AccountRoleGrantDao.class),
                mock(EmploymentRoleGrantDao.class), actionDao, activeTenantVerifier(), verifier,
                null, null, null, null);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThat(service.grantAction("r1", "sales.contract", "exportData")).isEqualTo(1);
        }

        verify(actionDao).insert(argThat(action -> "create".equals(action.getActionCode())));
    }

    @Test
    void shouldAuthorizeThroughEmploymentRoleGroupMembers() {
        RoleDao roleDao = mock(RoleDao.class);
        EmploymentRoleGrantDao employmentGrantDao = mock(EmploymentRoleGrantDao.class);
        RoleActionDao actionDao = mock(RoleActionDao.class);
        Role group = employmentRole("group-1", RoleKind.GROUP);
        group.setMemberRoleIds("r1");
        when(employmentGrantDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(employmentGrant("group-1", "position-1")));
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(group))
                .thenReturn(List.of(employmentRole("r1", RoleKind.STANDARD)))
                .thenReturn(List.of(group))
                .thenReturn(List.of(employmentRole("r1", RoleKind.STANDARD)))
                .thenReturn(List.of(employmentRole("r1", RoleKind.STANDARD)));
        when(actionDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(enabledAction("ra1", "r1", "sales.contract", "view")));
        EmployeePositionService employeePositionService = mock(EmployeePositionService.class);
        when(employeePositionService.select("position-1"))
                .thenReturn(employeePosition("position-1", "employee-1", "org-1", "dept-1", true));
        RoleService service = new RoleService(roleDao, mock(AccountRoleGrantDao.class), employmentGrantDao,
                actionDao, activeTenantVerifier(), RoleActionGrantVerifier.platformActionsOnly(),
                null, null, employeePositionService, null);

        BusinessPrincipal principal = BusinessPrincipal.employeePosition("employee-1", null, null, "position-1");
        assertThat(service.effectiveRoleIds(principal)).containsExactly("group-1", "r1");
        assertThat(service.hasActionPermission(principal, "sales.contract", "query")).isTrue();
    }

    @Test
    void shouldAggregateEffectiveRoleGrantsFromAccountAndEmployeePositions() {
        AccountRoleGrantDao accountGrantDao = mock(AccountRoleGrantDao.class);
        EmploymentRoleGrantDao employmentGrantDao = mock(EmploymentRoleGrantDao.class);
        RoleDao roleDao = mock(RoleDao.class);
        EmployeeAccountService employeeAccountService = mock(EmployeeAccountService.class);
        EmployeeService employeeService = mock(EmployeeService.class);
        EmployeePositionService employeePositionService = mock(EmployeePositionService.class);
        when(accountGrantDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(accountGrant("account-role", "user-1", ManagementScopeType.TENANT, "tenant_a")));
        when(employeeAccountService.employeeIdOfUser("user-1")).thenReturn("employee-1");
        when(employeeService.select("employee-1")).thenReturn(employee("employee-1", "org-main", "dept-main", true));
        when(employeePositionService.positions("employee-1"))
                .thenReturn(List.of(employeePosition("position-1", "employee-1", "org-branch", "dept-branch", true)));
        when(employmentGrantDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(employmentGrant("position-role", "position-1")));
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(accountRole("account-role", RoleKind.STANDARD)))
                .thenReturn(List.of(employmentRole("position-role", RoleKind.STANDARD)));
        RoleService service = new RoleService(roleDao, accountGrantDao, employmentGrantDao,
                mock(RoleActionDao.class), activeTenantVerifier(), RoleActionGrantVerifier.platformActionsOnly(),
                null, employeeService, employeePositionService, employeeAccountService);

        List<EffectiveRoleGrant> grants = service.effectiveRoleGrants("user-1");

        assertThat(grants).extracting(EffectiveRoleGrant::roleId)
                .containsExactly("account-role", "position-role");
        assertThat(grants.get(0))
                .extracting(EffectiveRoleGrant::sourceType, EffectiveRoleGrant::sourceId,
                        EffectiveRoleGrant::managementScopeType, EffectiveRoleGrant::managementScopeId)
                .containsExactly(RoleAssignmentType.ACCOUNT, "user-1", ManagementScopeType.TENANT, "tenant_a");
        assertThat(grants.get(1))
                .extracting(EffectiveRoleGrant::sourceType, EffectiveRoleGrant::sourceId,
                        EffectiveRoleGrant::organizationId, EffectiveRoleGrant::departmentId,
                        EffectiveRoleGrant::employeePositionId)
                .containsExactly(RoleAssignmentType.EMPLOYMENT, "position-1", "org-branch", "dept-branch", "position-1");
    }

    @Test
    void shouldResolveInheritedDataGrantActionForEmploymentContext() {
        RoleDao roleDao = mock(RoleDao.class);
        EmploymentRoleGrantDao employmentGrantDao = mock(EmploymentRoleGrantDao.class);
        RoleActionDao actionDao = mock(RoleActionDao.class);
        RoleAction grant = enabledAction("ra1", "data-role", "sales.contract", "view");
        grant.setDataScopePolicy(DataScopePolicy.ORGANIZATION);
        when(employmentGrantDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(employmentGrant("data-role", "position-1")));
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(employmentRole("data-role", RoleKind.DATA_GRANT)));
        when(actionDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(grant));
        RoleService service = service(roleDao, mock(AccountRoleGrantDao.class), employmentGrantDao, actionDao);

        RoleAction resolved = service.inheritedDataGrantAction(
                EffectiveRoleGrant.employment("standard-role", "position-1", "org-1", "dept-1"),
                "sales.contract", "query");

        assertThat(resolved).isSameAs(grant);
    }

    @Test
    void shouldRequireTenantContextWhenGrantingRoleFacts() {
        RoleDao roleDao = mock(RoleDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(accountRole("account-role", RoleKind.STANDARD)))
                .thenReturn(List.of(employmentRole("employment-role", RoleKind.STANDARD)));
        AccountRoleGrantDao accountGrantDao = mock(AccountRoleGrantDao.class);
        EmploymentRoleGrantDao employmentGrantDao = mock(EmploymentRoleGrantDao.class);
        when(accountGrantDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of());
        when(employmentGrantDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of());
        RoleService service = service(roleDao, accountGrantDao, employmentGrantDao, mock(RoleActionDao.class));

        assertThatThrownBy(() -> service.grantAccountRole(
                "account-role", "user-1", ManagementScopeType.TENANT, "tenant_a"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("iam.role management requires tenant context");
        assertThatThrownBy(() -> service.grantEmploymentRole("employment-role", "position-1"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("iam.role management requires tenant context");
        verify(accountGrantDao, never()).insert(any());
        verify(employmentGrantDao, never()).insert(any());
    }

    @Test
    void shouldRejectRoleStructuralFieldChangesAfterCreation() {
        RoleService service = spy(service(mock(RoleDao.class), mock(AccountRoleGrantDao.class),
                mock(EmploymentRoleGrantDao.class), mock(RoleActionDao.class)));
        doReturn(accountRole("role-1", RoleKind.STANDARD)).when(service).select("role-1");

        Role changedAssignment = employmentRole("role-1", RoleKind.STANDARD);
        assertThatThrownBy(() -> service.beforeUpdate(changedAssignment))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("role assignment type cannot be changed");

        Role changedKind = accountRole("role-1", RoleKind.SYSTEM);
        assertThatThrownBy(() -> service.beforeUpdate(changedKind))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("role kind cannot be changed");
    }

    @Test
    void shouldReturnEffectiveActionGrantsWithRoleGrantContext() {
        RoleActionDao roleActionDao = mock(RoleActionDao.class);
        RoleAction action = enabledAction("action-1", "position-role", "sales.contract", "view");
        when(roleActionDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(action));
        RoleService service = spy(service(mock(RoleDao.class), mock(AccountRoleGrantDao.class),
                mock(EmploymentRoleGrantDao.class), roleActionDao));
        doReturn(List.of(
                EffectiveRoleGrant.employment("position-role", "position-1", "org-branch", "dept-branch"),
                EffectiveRoleGrant.employment("position-role", "position-2", "org-other", "dept-other")
        )).when(service).effectiveRoleGrants("user-1");

        List<EffectiveRoleActionGrant> grants = service.effectiveActionGrantsWithContext(
                "user-1", "sales.contract", "query");

        assertThat(grants).hasSize(2);
        assertThat(grants).allSatisfy(grant -> assertThat(grant.actionGrant()).isSameAs(action));
        assertThat(grants).extracting(grant -> grant.roleGrant().employeePositionId())
                .containsExactly("position-1", "position-2");
    }

    @Test
    void shouldKeepLegacyEffectiveActionGrantsDistinctWhenRoleHasMultipleContexts() {
        RoleActionDao roleActionDao = mock(RoleActionDao.class);
        RoleAction action = enabledAction("action-1", "position-role", "sales.contract", "view");
        when(roleActionDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(action));
        RoleService service = spy(service(mock(RoleDao.class), mock(AccountRoleGrantDao.class),
                mock(EmploymentRoleGrantDao.class), roleActionDao));
        doReturn(List.of(
                EffectiveRoleGrant.employment("position-role", "position-1", "org-branch", "dept-branch"),
                EffectiveRoleGrant.employment("position-role", "position-2", "org-other", "dept-other")
        )).when(service).effectiveRoleGrants("user-1");

        List<RoleAction> grants = service.effectiveActionGrants("user-1", "sales.contract", "query");

        assertThat(grants).containsExactly(action);
    }

    @Test
    void shouldIgnoreDisabledEmployeeAndPositionWhenAggregatingEffectiveRoleGrants() {
        AccountRoleGrantDao accountGrantDao = mock(AccountRoleGrantDao.class);
        EmployeeAccountService employeeAccountService = mock(EmployeeAccountService.class);
        EmployeeService employeeService = mock(EmployeeService.class);
        EmployeePositionService employeePositionService = mock(EmployeePositionService.class);
        RoleDao roleDao = mock(RoleDao.class);
        when(accountGrantDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(accountGrant("account-role", "user-1", ManagementScopeType.TENANT, "tenant_a")));
        when(employeeAccountService.employeeIdOfUser("user-1")).thenReturn("employee-1");
        when(employeeService.select("employee-1")).thenReturn(employee("employee-1", "org-main", "dept-main", false));
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(accountRole("account-role", RoleKind.STANDARD)));
        RoleService service = new RoleService(roleDao, accountGrantDao, mock(EmploymentRoleGrantDao.class),
                mock(RoleActionDao.class), activeTenantVerifier(), RoleActionGrantVerifier.platformActionsOnly(),
                null, employeeService, employeePositionService, employeeAccountService);

        assertThat(service.effectiveRoleGrants("user-1")).extracting(EffectiveRoleGrant::roleId)
                .containsExactly("account-role");
    }

    @Test
    void shouldReturnAlignedActionViewWithDisabledMissingActions() {
        RoleActionDao actionDao = mock(RoleActionDao.class);
        when(actionDao.query(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(List.of(enabledAction("ra1", "r1", "sales.contract", "query")));
        RoleService service = service(mock(RoleDao.class), mock(AccountRoleGrantDao.class),
                mock(EmploymentRoleGrantDao.class), actionDao);

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
        when(roleDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(employmentRole("r1", RoleKind.STANDARD)));
        when(actionDao.query(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(List.of(viewGrant));
        RoleService service = service(roleDao, mock(AccountRoleGrantDao.class),
                mock(EmploymentRoleGrantDao.class), actionDao);

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
    void shouldRejectPermissionMatrixForRoleGroup() {
        RoleDao roleDao = mock(RoleDao.class);
        when(roleDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(employmentRole("group-1", RoleKind.GROUP)));
        RoleService service = service(roleDao, mock(AccountRoleGrantDao.class),
                mock(EmploymentRoleGrantDao.class), mock(RoleActionDao.class));

        assertThatThrownBy(() -> service.permissionMatrix("group-1", List.of(
                GrantableAction.ofPlatformDefaults("sales.contract", PlatformAction.QUERY)
        )))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("role group cannot be granted actions directly");
    }

    private RoleService service(RoleDao roleDao,
                                AccountRoleGrantDao accountRoleGrantDao,
                                EmploymentRoleGrantDao employmentRoleGrantDao,
                                RoleActionDao roleActionDao) {
        return new RoleService(roleDao, accountRoleGrantDao, employmentRoleGrantDao, roleActionDao,
                activeTenantVerifier());
    }

    private Role accountRole(String id, RoleKind kind) {
        return role(id, "Role " + id, RoleAssignmentType.ACCOUNT, kind);
    }

    private Role employmentRole(String id, RoleKind kind) {
        return role(id, "Role " + id, RoleAssignmentType.EMPLOYMENT, kind);
    }

    private Role systemManagedRole(String id) {
        Role role = accountRole(id, RoleKind.STANDARD);
        role.setSystemManaged(Boolean.TRUE);
        return role;
    }

    private Role role(String id, String title, RoleAssignmentType assignmentType, RoleKind kind) {
        Role role = new Role();
        role.setId(id);
        role.setTitle(title);
        role.setAssignmentType(assignmentType);
        role.setRoleKind(kind);
        role.setEnabled(Boolean.TRUE);
        return role;
    }

    private AccountRoleGrant accountGrant(String roleId,
                                          String userId,
                                          ManagementScopeType scopeType,
                                          String scopeId) {
        AccountRoleGrant grant = new AccountRoleGrant();
        grant.setRoleId(roleId);
        grant.setUserId(userId);
        grant.setManagementScopeType(scopeType);
        grant.setManagementScopeId(scopeId);
        grant.setEnabled(Boolean.TRUE);
        return grant;
    }

    private EmploymentRoleGrant employmentGrant(String roleId, String employeePositionId) {
        EmploymentRoleGrant grant = new EmploymentRoleGrant();
        grant.setRoleId(roleId);
        grant.setEmployeePositionId(employeePositionId);
        grant.setEnabled(Boolean.TRUE);
        return grant;
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
        action.setDataScopePolicy(DataScopePolicy.NONE);
        action.setEnabled(Boolean.TRUE);
        return action;
    }

    private ActiveTenantVerifier activeTenantVerifier() {
        return mock(ActiveTenantVerifier.class);
    }
}
