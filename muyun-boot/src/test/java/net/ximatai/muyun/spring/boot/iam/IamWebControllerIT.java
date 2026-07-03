package net.ximatai.muyun.spring.boot.iam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import net.ximatai.muyun.spring.boot.platform.StaticModuleDefinitionCatalog;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserProvider;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.AllowAllActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.iam.department.Department;
import net.ximatai.muyun.spring.iam.department.DepartmentService;
import net.ximatai.muyun.spring.iam.employee.EmployeeAccount;
import net.ximatai.muyun.spring.iam.employee.EmployeeAccountService;
import net.ximatai.muyun.spring.iam.employee.EmployeeDelegation;
import net.ximatai.muyun.spring.iam.employee.EmployeeDelegationService;
import net.ximatai.muyun.spring.iam.employee.EmployeePosition;
import net.ximatai.muyun.spring.iam.employee.EmployeePositionService;
import net.ximatai.muyun.spring.iam.employee.EmployeeService;
import net.ximatai.muyun.spring.iam.organization.Organization;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import net.ximatai.muyun.spring.iam.position.PositionCategoryService;
import net.ximatai.muyun.spring.iam.position.PositionService;
import net.ximatai.muyun.spring.iam.role.DataScopePolicy;
import net.ximatai.muyun.spring.iam.role.ManagementScopeType;
import net.ximatai.muyun.spring.iam.role.RolePermissionMatrix;
import net.ximatai.muyun.spring.iam.role.RoleService;
import net.ximatai.muyun.spring.iam.role.TenantScopePolicy;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(IamWebControllerIT.WebProfile.class)
class IamWebControllerIT {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TestHTTPResource
    URI baseUri;

    private HttpClient httpClient;

    @BeforeEach
    void setUp() {
        TestBeans.reset();
        httpClient = HttpClient.newHttpClient();
        when(TestBeans.currentUserProvider.currentUser())
                .thenReturn(Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant_a")));
    }

    @Test
    void shouldBindInheritedOrganizationTreeRouteInRealQuarkusHttpContext() throws Exception {
        Organization organization = new Organization();
        organization.setId("org-1");
        organization.setCode("HQ");
        organization.setTitle("Headquarters");
        organization.setParentId("root");
        when(TestBeans.organizationService.childrenForAction(PlatformAction.TREE, "root"))
                .thenReturn(List.of(organization));
        when(TestBeans.organizationService.childrenForAction(PlatformAction.TREE, "org-1"))
                .thenReturn(List.of());

        HttpResponse<String> response = get("/iam.organization/tree");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json(response).at("/records/0/record/id").asText()).isEqualTo("org-1");
    }

    @Test
    void shouldBindDepartmentTreeEndpointInRealQuarkusHttpContext() throws Exception {
        Department department = new Department();
        department.setId("dept-1");
        department.setOrganizationId("org-1");
        department.setCode("FIN");
        department.setTitle("Finance");
        department.setParentId("root");
        when(TestBeans.departmentService.departmentChildrenForAction(PlatformAction.TREE, "org-1", "root"))
                .thenReturn(List.of(department));
        when(TestBeans.departmentService.departmentChildrenForAction(PlatformAction.TREE, "org-1", "dept-1"))
                .thenReturn(List.of());

        HttpResponse<String> response = get("/iam.department/tree?organizationId=org-1");

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode first = json(response).at("/records/0");
        assertThat(first.at("/record/id").asText()).isEqualTo("dept-1");
        assertThat(first.at("/record/organizationId").asText()).isEqualTo("org-1");
        assertThat(first.get("children").isArray()).isTrue();
    }

    @Test
    void shouldBindDepartmentTreeSortEndpointInRealQuarkusHttpContext() throws Exception {
        HttpResponse<String> response = post("/iam.department/sort/dept-1", """
                {"previousId":"dept-0","parentId":"root"}
                """);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json(response).get("count").asInt()).isEqualTo(1);
        verify(TestBeans.departmentService).moveInDepartmentTree("dept-1", "dept-0", null, "root");
    }

    @Test
    void shouldBindEmployeePositionEndpointsInRealQuarkusHttpContext() throws Exception {
        EmployeePosition relation = employeePosition("relation-1");
        when(TestBeans.employeePositionService.positions("employee-1")).thenReturn(List.of(relation));
        when(TestBeans.employeePositionService.addPosition(eq("employee-1"), any(EmployeePosition.class)))
                .thenReturn("relation-1");
        when(TestBeans.employeePositionService.select("relation-1")).thenReturn(relation);
        when(TestBeans.employeePositionService.deletePosition("employee-1", "relation-1")).thenReturn(1);
        when(TestBeans.employeePositionService.makePrimaryPosition("employee-1", "relation-1")).thenReturn(1);

        assertThat(json(get("/iam.employee/employee-1/positions")).at("/records/0/id").asText())
                .isEqualTo("relation-1");
        assertThat(json(post("/iam.employee/employee-1/positions", """
                {"organizationId":"org-1","departmentId":"dept-1","positionId":"position-1"}
                """)).get("id").asText()).isEqualTo("relation-1");
        assertThat(json(post("/iam.employee/employee-1/positions/relation-1/delete", "{}"))
                .get("count").asInt()).isEqualTo(1);
        assertThat(json(post("/iam.employee/employee-1/positions/relation-1/primary", "{}"))
                .get("count").asInt()).isEqualTo(1);
    }

    @Test
    void shouldBindEmployeeAccountEndpointsInRealQuarkusHttpContext() throws Exception {
        EmployeeAccount binding = new EmployeeAccount();
        binding.setId("binding-1");
        binding.setEmployeeId("employee-1");
        binding.setUserId("user-2");
        binding.setPrimaryAccount(Boolean.TRUE);
        when(TestBeans.employeeAccountService.accounts("employee-1")).thenReturn(List.of(binding));
        when(TestBeans.employeeAccountService.bindAccount(eq("employee-1"), any(EmployeeAccount.class)))
                .thenReturn("binding-1");
        when(TestBeans.employeeAccountService.select("binding-1")).thenReturn(binding);
        when(TestBeans.employeeAccountService.deleteAccount("employee-1", "binding-1")).thenReturn(1);

        assertThat(json(get("/iam.employee/employee-1/accounts")).at("/records/0/id").asText())
                .isEqualTo("binding-1");
        assertThat(json(post("/iam.employee/employee-1/accounts", """
                {"userId":"user-2","primaryAccount":true}
                """)).get("userId").asText()).isEqualTo("user-2");
        assertThat(json(post("/iam.employee/employee-1/accounts/binding-1/delete", "{}"))
                .get("count").asInt()).isEqualTo(1);
    }

    @Test
    void shouldBindEmployeeDelegationEndpointsInRealQuarkusHttpContext() throws Exception {
        EmployeeDelegation delegation = new EmployeeDelegation();
        delegation.setId("delegation-1");
        delegation.setPrincipalEmployeeId("employee-1");
        delegation.setDelegateEmployeeId("employee-2");
        when(TestBeans.employeeDelegationService.delegationsByPrincipal("employee-1"))
                .thenReturn(List.of(delegation));
        when(TestBeans.employeeDelegationService.delegationsByDelegate("employee-2"))
                .thenReturn(List.of(delegation));
        when(TestBeans.employeeDelegationService.addDelegation(eq("employee-1"), any(EmployeeDelegation.class)))
                .thenReturn("delegation-1");
        when(TestBeans.employeeDelegationService.select("delegation-1")).thenReturn(delegation);
        when(TestBeans.employeeDelegationService.deleteDelegation("employee-1", "delegation-1")).thenReturn(1);

        assertThat(json(get("/iam.employee/employee-1/delegations")).at("/records/0/id").asText())
                .isEqualTo("delegation-1");
        assertThat(json(get("/iam.employee/employee-2/delegated-to-me")).at("/records/0/principalEmployeeId").asText())
                .isEqualTo("employee-1");
        assertThat(json(post("/iam.employee/employee-1/delegations", """
                {"delegateEmployeeId":"employee-2"}
                """)).get("id").asText()).isEqualTo("delegation-1");
        assertThat(json(post("/iam.employee/employee-1/delegations/delegation-1/delete", "{}"))
                .get("count").asInt()).isEqualTo(1);
    }

    @Test
    void shouldBindRolePermissionEndpointsInRealQuarkusHttpContext() throws Exception {
        when(TestBeans.roleService.grantAccountRole("role-1", "user-2",
                ManagementScopeType.TENANT, null)).thenReturn("grant-1");
        when(TestBeans.roleService.deleteAccountRoleGrant("role-1", "grant-1")).thenReturn(1);
        when(TestBeans.roleService.grantAction("role-1", "iam.employee", "view",
                DataScopePolicy.ALL, TenantScopePolicy.CURRENT_TENANT, null, null, null)).thenReturn(1);
        when(TestBeans.roleGrantableActionResolver.resolve(List.of("iam.employee")))
                .thenReturn(List.of());
        when(TestBeans.roleService.permissionMatrix(eq("role-1"), any()))
                .thenReturn(new RolePermissionMatrix("role-1", List.of()));

        assertThat(post("/iam.role/role-1/account-grants", """
                {"userId":"user-2","managementScopeType":"TENANT"}
                """).body()).isEqualTo("grant-1");
        assertThat(json(post("/iam.role/role-1/account-grants/grant-1/delete", "{}"))
                .get("count").asInt()).isEqualTo(1);
        assertThat(json(post("/iam.role/grant/role-1", """
                {
                  "moduleAlias":"iam.employee",
                  "actionCode":"view",
                  "dataScopePolicy":"ALL",
                  "tenantScopePolicy":"CURRENT_TENANT"
                }
                """)).get("count").asInt()).isEqualTo(1);
        assertThat(json(post("/iam.role/permissionMatrix/role-1", """
                {"moduleAliases":["iam.employee"]}
                """)).get("roleId").asText()).isEqualTo("role-1");
    }

    private HttpResponse<String> get(String path) throws Exception {
        return httpClient.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        return httpClient.send(HttpRequest.newBuilder(uri(path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return baseUri.resolve(path.substring(1));
    }

    private JsonNode json(HttpResponse<String> response) throws Exception {
        assertThat(response.statusCode()).isEqualTo(200);
        return JSON.readTree(response.body());
    }

    private EmployeePosition employeePosition(String id) {
        EmployeePosition relation = new EmployeePosition();
        relation.setId(id);
        relation.setEmployeeId("employee-1");
        relation.setOrganizationId("org-1");
        relation.setDepartmentId("dept-1");
        relation.setPositionId("position-1");
        relation.setPrimaryPosition(Boolean.TRUE);
        return relation;
    }

    @Alternative
    @Priority(1)
    @ApplicationScoped
    public static class TestBeans {
        static TenantService tenantService = Mockito.mock(TenantService.class);
        static OrganizationService organizationService = Mockito.mock(OrganizationService.class);
        static DepartmentService departmentService = Mockito.mock(DepartmentService.class);
        static EmployeeService employeeService = Mockito.mock(EmployeeService.class);
        static EmployeePositionService employeePositionService = Mockito.mock(EmployeePositionService.class);
        static EmployeeAccountService employeeAccountService = Mockito.mock(EmployeeAccountService.class);
        static EmployeeDelegationService employeeDelegationService = Mockito.mock(EmployeeDelegationService.class);
        static PositionService positionService = Mockito.mock(PositionService.class);
        static PositionCategoryService positionCategoryService = Mockito.mock(PositionCategoryService.class);
        static RoleService roleService = Mockito.mock(RoleService.class);
        static RoleGrantableActionResolver roleGrantableActionResolver = Mockito.mock(RoleGrantableActionResolver.class);
        static PlatformModuleActionService moduleActionService = Mockito.mock(PlatformModuleActionService.class);
        static StaticModuleDefinitionCatalog staticModuleDefinitionCatalog =
                Mockito.mock(StaticModuleDefinitionCatalog.class);
        static CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        static MenuService menuService = Mockito.mock(MenuService.class);

        static void reset() {
            Mockito.reset(tenantService, organizationService, departmentService, employeeService,
                    employeePositionService, employeeAccountService, employeeDelegationService, positionService,
                    positionCategoryService, roleService, roleGrantableActionResolver, moduleActionService,
                    staticModuleDefinitionCatalog, currentUserProvider, menuService);
        }

        @Produces
        @Dependent
        TenantService tenantService() {
            return tenantService;
        }

        @Produces
        @Dependent
        OrganizationService organizationService() {
            return organizationService;
        }

        @Produces
        @Dependent
        DepartmentService departmentService() {
            return departmentService;
        }

        @Produces
        @Dependent
        EmployeeService employeeService() {
            return employeeService;
        }

        @Produces
        @Dependent
        EmployeePositionService employeePositionService() {
            return employeePositionService;
        }

        @Produces
        @Dependent
        EmployeeAccountService employeeAccountService() {
            return employeeAccountService;
        }

        @Produces
        @Dependent
        EmployeeDelegationService employeeDelegationService() {
            return employeeDelegationService;
        }

        @Produces
        @Dependent
        PositionService positionService() {
            return positionService;
        }

        @Produces
        @Dependent
        PositionCategoryService positionCategoryService() {
            return positionCategoryService;
        }

        @Produces
        @Dependent
        RoleService roleService() {
            return roleService;
        }

        @Produces
        @Dependent
        RoleGrantableActionResolver roleGrantableActionResolver() {
            return roleGrantableActionResolver;
        }

        @Produces
        @Dependent
        PlatformModuleActionService moduleActionService() {
            return moduleActionService;
        }

        @Produces
        @Dependent
        StaticModuleDefinitionCatalog staticModuleDefinitionCatalog() {
            return staticModuleDefinitionCatalog;
        }

        @Produces
        @Dependent
        CurrentUserProvider currentUserProvider() {
            return currentUserProvider;
        }

        @Produces
        @Dependent
        ActionExecutionPolicyService actionExecutionPolicyService() {
            return new AllowAllActionExecutionPolicyService();
        }

        @Produces
        @Dependent
        MenuService menuService() {
            return menuService;
        }
    }

    public static class WebProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> config = new HashMap<>();
            config.put("quarkus.datasource.db-kind", "postgresql");
            config.put("quarkus.datasource.devservices.enabled", "false");
            config.put("quarkus.datasource.jdbc.url", "jdbc:postgresql://localhost:1/muyun_iam_web_it");
            config.put("quarkus.datasource.username", "testuser");
            config.put("quarkus.datasource.password", "testpass");
            config.put("muyun.database.repository-schema-mode", "NONE");
            config.put("muyun.platform.time.default-zone-id", "Asia/Shanghai");
            config.put("quarkus.arc.remove-unused-beans", "false");
            config.put("quarkus.arc.exclude-types", String.join(",",
                    "net.ximatai.muyun.spring.boot.dynamic.DynamicRecordWebControllerIT$NoopTenantService",
                    "net.ximatai.muyun.spring.iam.tenant.TenantService",
                    "net.ximatai.muyun.spring.iam.organization.OrganizationService",
                    "net.ximatai.muyun.spring.iam.department.DepartmentService",
                    "net.ximatai.muyun.spring.iam.employee.EmployeeService",
                    "net.ximatai.muyun.spring.iam.employee.EmployeePositionService",
                    "net.ximatai.muyun.spring.iam.employee.EmployeeAccountService",
                    "net.ximatai.muyun.spring.iam.employee.EmployeeDelegationService",
                    "net.ximatai.muyun.spring.iam.position.PositionService",
                    "net.ximatai.muyun.spring.iam.position.PositionCategoryService",
                    "net.ximatai.muyun.spring.iam.role.RoleService",
                    "net.ximatai.muyun.spring.boot.iam.RoleGrantableActionResolver",
                    "net.ximatai.muyun.spring.platform.module.PlatformModuleActionService",
                    "net.ximatai.muyun.spring.boot.dynamic.DynamicRecordWebControllerIT$TestBeans"
            ));
            return config;
        }
    }
}
