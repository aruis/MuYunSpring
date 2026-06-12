package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.boot.web.CurrentUserWebFilter;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserProvider;
import net.ximatai.muyun.spring.iam.department.Department;
import net.ximatai.muyun.spring.iam.department.DepartmentService;
import net.ximatai.muyun.spring.iam.employee.Employee;
import net.ximatai.muyun.spring.iam.employee.EmployeeAccount;
import net.ximatai.muyun.spring.iam.employee.EmployeeAccountService;
import net.ximatai.muyun.spring.iam.employee.EmployeeDelegation;
import net.ximatai.muyun.spring.iam.employee.EmployeeDelegationService;
import net.ximatai.muyun.spring.iam.employee.EmployeePosition;
import net.ximatai.muyun.spring.iam.employee.EmployeePositionService;
import net.ximatai.muyun.spring.iam.employee.EmployeeService;
import net.ximatai.muyun.spring.iam.organization.Organization;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import net.ximatai.muyun.spring.iam.position.Position;
import net.ximatai.muyun.spring.iam.position.PositionService;
import net.ximatai.muyun.spring.iam.role.DataScopePolicy;
import net.ximatai.muyun.spring.iam.role.GrantableAction;
import net.ximatai.muyun.spring.iam.role.RoleGrant;
import net.ximatai.muyun.spring.iam.role.RoleGrantSubjectType;
import net.ximatai.muyun.spring.iam.role.RolePermissionAction;
import net.ximatai.muyun.spring.iam.role.RolePermissionMatrix;
import net.ximatai.muyun.spring.iam.role.RoleService;
import net.ximatai.muyun.spring.iam.role.TenantScopePolicy;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        TenantWebController.class,
        OrganizationWebController.class,
        DepartmentWebController.class,
        EmployeeWebController.class,
        PositionWebController.class,
        RoleWebController.class
})
@Import({
        CurrentUserWebFilter.class,
        IamWebExceptionHandler.class
})
class IamWebControllerIT {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private OrganizationService organizationService;

    @MockitoBean
    private DepartmentService departmentService;

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private EmployeePositionService employeePositionService;

    @MockitoBean
    private EmployeeAccountService employeeAccountService;

    @MockitoBean
    private EmployeeDelegationService employeeDelegationService;

    @MockitoBean
    private PositionService positionService;

    @MockitoBean
    private RoleService roleService;

    @MockitoBean
    private RoleGrantableActionResolver roleGrantableActionResolver;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void shouldUseInjectedServiceAndCurrentUserTenantInRealMvcContext() throws Exception {
        Organization organization = new Organization();
        organization.setId("org-1");
        organization.setCode("HQ");
        organization.setTitle("Headquarters");
        organization.setParentId(TreeAbility.ROOT_ID);
        when(currentUserProvider.currentUser())
                .thenReturn(Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant_a")));
        when(organizationService.children(TreeAbility.ROOT_ID)).thenReturn(List.of(organization));
        when(organizationService.children("org-1")).thenReturn(List.of());

        mvc.perform(get("/iam.organization/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].record.id").value("org-1"))
                .andExpect(jsonPath("$.records[0].children").isArray());
    }

    @Test
    void shouldBindTreeSortEndpointInRealMvcContext() throws Exception {
        when(currentUserProvider.currentUser())
                .thenReturn(Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant_a")));

        mvc.perform(post("/iam.organization/sort/org-1")
                        .contentType("application/json")
                        .content("""
                                {"previousId":"org-0","parentId":"root"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        verify(organizationService).moveInTree("org-1", "org-0", null, TreeAbility.ROOT_ID);
    }

    @Test
    void shouldBindDepartmentTreeEndpointWithOrganizationScopeInRealMvcContext() throws Exception {
        Department department = new Department();
        department.setId("dept-1");
        department.setOrganizationId("org-1");
        department.setCode("FIN");
        department.setTitle("Finance");
        department.setParentId(TreeAbility.ROOT_ID);
        when(currentUserProvider.currentUser())
                .thenReturn(Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant_a")));
        when(departmentService.rootDepartments("org-1")).thenReturn(List.of(department));
        when(departmentService.departmentChildren("org-1", "dept-1")).thenReturn(List.of());

        mvc.perform(get("/iam.department/tree").param("organizationId", "org-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].record.id").value("dept-1"))
                .andExpect(jsonPath("$.records[0].record.organizationId").value("org-1"))
                .andExpect(jsonPath("$.records[0].children").isArray());
    }

    @Test
    void shouldBindDepartmentTreeSortEndpointInRealMvcContext() throws Exception {
        when(currentUserProvider.currentUser())
                .thenReturn(Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant_a")));

        mvc.perform(post("/iam.department/sort/dept-1")
                        .contentType("application/json")
                        .content("""
                                {"previousId":"dept-0","parentId":"root"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        verify(departmentService).moveInDepartmentTree("dept-1", "dept-0", null, TreeAbility.ROOT_ID);
    }

    @Test
    void shouldBindPlainSortEndpointInRealMvcContext() throws Exception {
        when(currentUserProvider.currentUser())
                .thenReturn(Optional.of(CurrentUser.systemUser("admin", "Admin")));

        mvc.perform(post("/iam.tenant/sort/tenant-1")
                        .contentType("application/json")
                        .content("""
                                {"previousId":"tenant-0"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        verify(tenantService).moveAfter("tenant-1", "tenant-0");
    }

    @Test
    void shouldBindEmployeeSortEndpointInRealMvcContext() throws Exception {
        when(currentUserProvider.currentUser())
                .thenReturn(Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant_a")));

        mvc.perform(post("/iam.employee/sort/employee-1")
                        .contentType("application/json")
                        .content("""
                                {"previousId":"employee-0"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        verify(employeeService).moveAfter("employee-1", "employee-0");
    }

    @Test
    void shouldBindEmployeeViewEndpointInRealMvcContext() throws Exception {
        Employee employee = new Employee();
        employee.setId("employee-1");
        employee.setOrganizationId("org-1");
        employee.setDepartmentId("dept-1");
        employee.setEmployeeNo("E001");
        employee.setTitle("Alice");
        when(currentUserProvider.currentUser())
                .thenReturn(Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant_a")));
        when(employeeService.select("employee-1")).thenReturn(employee);

        mvc.perform(get("/iam.employee/view/employee-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("employee-1"))
                .andExpect(jsonPath("$.departmentId").value("dept-1"));
    }

    @Test
    void shouldBindEmployeePositionEndpointsInRealMvcContext() throws Exception {
        EmployeePosition relation = new EmployeePosition();
        relation.setId("relation-1");
        relation.setEmployeeId("employee-1");
        relation.setOrganizationId("org-1");
        relation.setDepartmentId("dept-1");
        relation.setPositionId("position-1");
        relation.setPrimaryPosition(Boolean.TRUE);
        when(currentUserProvider.currentUser())
                .thenReturn(Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant_a")));
        when(employeePositionService.positions("employee-1")).thenReturn(List.of(relation));
        when(employeePositionService.addPosition(eq("employee-1"), any())).thenReturn("relation-1");
        when(employeePositionService.select("relation-1")).thenReturn(relation);
        when(employeePositionService.deletePosition("employee-1", "relation-1")).thenReturn(1);
        when(employeePositionService.makePrimaryPosition("employee-1", "relation-1")).thenReturn(1);

        mvc.perform(get("/iam.employee/employee-1/positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].id").value("relation-1"))
                .andExpect(jsonPath("$.records[0].primaryPosition").value(true));

        mvc.perform(post("/iam.employee/employee-1/positions")
                        .contentType("application/json")
                        .content("""
                                {"organizationId":"org-1","departmentId":"dept-1","positionId":"position-1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("relation-1"));

        mvc.perform(post("/iam.employee/employee-1/positions/relation-1/delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        mvc.perform(post("/iam.employee/employee-1/positions/relation-1/primary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void shouldBindEmployeePositionSortEndpointInRealMvcContext() throws Exception {
        when(currentUserProvider.currentUser())
                .thenReturn(Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant_a")));

        mvc.perform(post("/iam.employee/employee-1/positions/relation-1/sort")
                        .contentType("application/json")
                        .content("""
                                {"previousId":"relation-0"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        verify(employeePositionService).moveEmployeePosition("employee-1", "relation-1", "relation-0", null);
    }

    @Test
    void shouldBindEmployeeAccountEndpointsInRealMvcContext() throws Exception {
        EmployeeAccount binding = employeeAccount("binding-1", "employee-1", "user-2", true);
        when(currentUserProvider.currentUser())
                .thenReturn(Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant_a")));
        when(employeeAccountService.accounts("employee-1")).thenReturn(List.of(binding));
        when(employeeAccountService.bindAccount(eq("employee-1"), any(EmployeeAccount.class))).thenReturn("binding-1");
        when(employeeAccountService.select("binding-1")).thenReturn(binding);
        when(employeeAccountService.deleteAccount("employee-1", "binding-1")).thenReturn(1);
        when(employeeAccountService.enableAccount("employee-1", "binding-1")).thenReturn(1);
        when(employeeAccountService.disableAccount("employee-1", "binding-1")).thenReturn(1);
        when(employeeAccountService.makePrimaryAccount("employee-1", "binding-1")).thenReturn(1);

        mvc.perform(get("/iam.employee/employee-1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].id").value("binding-1"))
                .andExpect(jsonPath("$.records[0].primaryAccount").value(true));

        mvc.perform(post("/iam.employee/employee-1/accounts")
                        .contentType("application/json")
                        .content("""
                                {"userId":"user-2","primaryAccount":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("binding-1"))
                .andExpect(jsonPath("$.userId").value("user-2"));

        mvc.perform(post("/iam.employee/employee-1/accounts/binding-1/delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
        mvc.perform(post("/iam.employee/employee-1/accounts/binding-1/enable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
        mvc.perform(post("/iam.employee/employee-1/accounts/binding-1/disable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
        mvc.perform(post("/iam.employee/employee-1/accounts/binding-1/primary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        verify(employeeAccountService).accounts("employee-1");
        verify(employeeAccountService).bindAccount(eq("employee-1"), any(EmployeeAccount.class));
    }

    @Test
    void shouldBindEmployeeDelegationEndpointsInRealMvcContext() throws Exception {
        EmployeeDelegation delegation = employeeDelegation("delegation-1", "employee-1", "employee-2");
        when(currentUserProvider.currentUser())
                .thenReturn(Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant_a")));
        when(employeeDelegationService.delegationsByPrincipal("employee-1")).thenReturn(List.of(delegation));
        when(employeeDelegationService.delegationsByDelegate("employee-2")).thenReturn(List.of(delegation));
        when(employeeDelegationService.addDelegation(eq("employee-1"), any(EmployeeDelegation.class)))
                .thenReturn("delegation-1");
        when(employeeDelegationService.select("delegation-1")).thenReturn(delegation);
        when(employeeDelegationService.updateDelegation(eq("employee-1"), eq("delegation-1"),
                any(EmployeeDelegation.class))).thenReturn(1);
        when(employeeDelegationService.deleteDelegation("employee-1", "delegation-1")).thenReturn(1);
        when(employeeDelegationService.enableDelegation("employee-1", "delegation-1")).thenReturn(1);
        when(employeeDelegationService.disableDelegation("employee-1", "delegation-1")).thenReturn(1);

        mvc.perform(get("/iam.employee/employee-1/delegations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].id").value("delegation-1"))
                .andExpect(jsonPath("$.records[0].delegateEmployeeId").value("employee-2"));

        mvc.perform(get("/iam.employee/employee-2/delegated-to-me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].principalEmployeeId").value("employee-1"));

        mvc.perform(post("/iam.employee/employee-1/delegations")
                        .contentType("application/json")
                        .content("""
                                {"delegateEmployeeId":"employee-2"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("delegation-1"));

        mvc.perform(post("/iam.employee/employee-1/delegations/delegation-1/update")
                        .contentType("application/json")
                        .content("""
                                {"delegateEmployeeId":"employee-2","memo":"changed"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("delegation-1"));

        mvc.perform(post("/iam.employee/employee-1/delegations/delegation-1/delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
        mvc.perform(post("/iam.employee/employee-1/delegations/delegation-1/enable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
        mvc.perform(post("/iam.employee/employee-1/delegations/delegation-1/disable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        verify(employeeDelegationService).delegationsByPrincipal("employee-1");
        verify(employeeDelegationService).delegationsByDelegate("employee-2");
        verify(employeeDelegationService).addDelegation(eq("employee-1"), any(EmployeeDelegation.class));
    }

    @Test
    void shouldBindPositionEndpointInRealMvcContext() throws Exception {
        Position position = new Position();
        position.setId("position-1");
        position.setCode("SALES_MANAGER");
        position.setTitle("Sales Manager");
        when(currentUserProvider.currentUser())
                .thenReturn(Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant_a")));
        when(positionService.select("position-1")).thenReturn(position);

        mvc.perform(get("/iam.position/view/position-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("position-1"))
                .andExpect(jsonPath("$.code").value("SALES_MANAGER"));
    }

    @Test
    void shouldRejectPostForReadOnlyTreeEndpointInRealMvcContext() throws Exception {
        mvc.perform(post("/iam.organization/tree"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void shouldApplyAdviceWhenCurrentUserTenantIsMissingInRealMvcContext() throws Exception {
        when(currentUserProvider.currentUser()).thenReturn(Optional.empty());

        mvc.perform(get("/iam.organization/tree"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IAM_BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("iam.organization requires tenant context"));
    }

    @Test
    void shouldBindRoleManagementEndpointsInRealMvcContext() throws Exception {
        when(currentUserProvider.currentUser())
                .thenReturn(Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant_a")));
        RoleGrant grant = roleGrant("grant-1", "role-1", RoleGrantSubjectType.USER_ACCOUNT, "user-2");
        when(roleService.grantRole("role-1", RoleGrantSubjectType.USER_ACCOUNT, "user-2")).thenReturn("grant-1");
        when(roleService.roleGrants("role-1")).thenReturn(List.of(grant));
        when(roleService.deleteGrant("role-1", "grant-1")).thenReturn(1);
        when(roleService.grantAction("role-1", "sales.contract", "query",
                DataScopePolicy.OWNER, TenantScopePolicy.CURRENT_TENANT,
                null, null, null)).thenReturn(1);
        when(roleService.grantAction("role-1", "sales.contract", "query",
                DataScopePolicy.DEPARTMENT_AND_CHILDREN, TenantScopePolicy.CURRENT_TENANT,
                null, null, null)).thenReturn(1);
        when(roleService.revokeAction("role-1", "sales.contract", "query")).thenReturn(1);

        mvc.perform(post("/iam.role/{roleId}/grants", "role-1")
                        .contentType("application/json")
                        .content("""
                                {"subjectType":"USER_ACCOUNT","subjectId":"user-2"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("grant-1"));

        mvc.perform(get("/iam.role/{roleId}/grants", "role-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("grant-1"))
                .andExpect(jsonPath("$[0].subjectType").value("USER_ACCOUNT"))
                .andExpect(jsonPath("$[0].subjectId").value("user-2"));

        mvc.perform(post("/iam.role/{roleId}/grants/{grantId}/delete", "role-1", "grant-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        mvc.perform(post("/iam.role/grant/{roleId}", "role-1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "moduleAlias":"sales.contract",
                                  "actionCode":"query",
                                  "dataScopePolicy":"OWNER",
                                  "tenantScopePolicy":"CURRENT_TENANT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        mvc.perform(post("/iam.role/grant/{roleId}", "role-1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "moduleAlias":"sales.contract",
                                  "actionCode":"query",
                                  "dataScopePolicy":"DEPARTMENT_AND_CHILDREN",
                                  "tenantScopePolicy":"CURRENT_TENANT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        mvc.perform(post("/iam.role/revoke/{roleId}", "role-1")
                        .contentType("application/json")
                        .content("""
                                {"moduleAlias":"sales.contract","actionCode":"query"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void shouldResolveRolePermissionMatrixInRealMvcContext() throws Exception {
        when(currentUserProvider.currentUser())
                .thenReturn(Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant_a")));
        List<GrantableAction> grantableActions = List.of(
                new GrantableAction("sales.contract", "query", "view", "Query", true, true)
        );
        when(roleGrantableActionResolver.resolve(List.of("sales.contract"))).thenReturn(grantableActions);
        when(roleService.permissionMatrix("role-1", grantableActions)).thenReturn(new RolePermissionMatrix(
                "role-1",
                List.of(new RolePermissionMatrix.Module(
                        "sales.contract",
                        List.of(new RolePermissionAction(
                                "sales.contract", "query", "view", "Query",
                                true, true, true, DataScopePolicy.OWNER,
                                TenantScopePolicy.CURRENT_TENANT, null, null, null))
                ))
        ));

        mvc.perform(post("/iam.role/permissionMatrix/{roleId}", "role-1")
                        .contentType("application/json")
                        .content("""
                                {"moduleAliases":["sales.contract"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleId").value("role-1"))
                .andExpect(jsonPath("$.modules[0].moduleAlias").value("sales.contract"))
                .andExpect(jsonPath("$.modules[0].actions[0].permissionActionCode").value("view"))
                .andExpect(jsonPath("$.modules[0].actions[0].granted").value(true));
    }

    @Test
    void shouldApplyIamAdviceWhenRoleGrantRejectsUnsupportedCustomDataScope() throws Exception {
        when(currentUserProvider.currentUser())
                .thenReturn(Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant_a")));
        when(roleService.grantAction("role-1", "sales.contract", "query",
                DataScopePolicy.CUSTOM, TenantScopePolicy.CURRENT_TENANT,
                "authUserId = ${userId}", null, null))
                .thenThrow(new PlatformException("custom data scope policy is not supported yet"));

        mvc.perform(post("/iam.role/grant/{roleId}", "role-1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "moduleAlias":"sales.contract",
                                  "actionCode":"query",
                                  "dataScopePolicy":"CUSTOM",
                                  "tenantScopePolicy":"CURRENT_TENANT",
                                  "scopeCondition":"authUserId = ${userId}"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IAM_BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("custom data scope policy is not supported yet"));
    }

    private RoleGrant roleGrant(String id, String roleId, RoleGrantSubjectType subjectType, String subjectId) {
        RoleGrant grant = new RoleGrant();
        grant.setId(id);
        grant.setRoleId(roleId);
        grant.setSubjectType(subjectType);
        grant.setSubjectId(subjectId);
        grant.setEnabled(Boolean.TRUE);
        return grant;
    }

    private EmployeeAccount employeeAccount(String id, String employeeId, String userId, boolean primaryAccount) {
        EmployeeAccount binding = new EmployeeAccount();
        binding.setId(id);
        binding.setEmployeeId(employeeId);
        binding.setUserId(userId);
        binding.setPrimaryAccount(primaryAccount);
        binding.setEnabled(Boolean.TRUE);
        return binding;
    }

    private EmployeeDelegation employeeDelegation(String id, String principalEmployeeId, String delegateEmployeeId) {
        EmployeeDelegation delegation = new EmployeeDelegation();
        delegation.setId(id);
        delegation.setPrincipalEmployeeId(principalEmployeeId);
        delegation.setDelegateEmployeeId(delegateEmployeeId);
        delegation.setEnabled(Boolean.TRUE);
        return delegation;
    }
}
