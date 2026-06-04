package net.ximatai.muyun.spring.boot.iam;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.boot.web.CurrentUserWebFilter;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.iam.organization.Organization;
import net.ximatai.muyun.spring.iam.organization.OrganizationDao;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import net.ximatai.muyun.spring.iam.role.DataScopePolicy;
import net.ximatai.muyun.spring.iam.role.GrantableAction;
import net.ximatai.muyun.spring.iam.role.RolePermissionMatrix;
import net.ximatai.muyun.spring.iam.role.RoleService;
import net.ximatai.muyun.spring.iam.role.TenantScopePolicy;
import net.ximatai.muyun.spring.iam.tenant.Tenant;
import net.ximatai.muyun.spring.iam.tenant.TenantDao;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IamWebControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private TenantDao tenantDao;
    private OrganizationDao organizationDao;
    private RoleService roleService;
    private RoleGrantableActionResolver grantableActionResolver;
    private CurrentUser currentUser;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        currentUser = null;
        tenantDao = mock(TenantDao.class);
        organizationDao = mock(OrganizationDao.class);
        roleService = mock(RoleService.class);
        grantableActionResolver = mock(RoleGrantableActionResolver.class);
        TenantService tenantService = new TenantService(tenantDao);
        OrganizationService organizationService = new OrganizationService(organizationDao, tenantService);
        TenantWebController tenantController = new TenantWebController();
        OrganizationWebController organizationController = new OrganizationWebController();
        RoleWebController roleController = new RoleWebController(grantableActionResolver);
        ReflectionTestUtils.setField(tenantController, "service", tenantService);
        ReflectionTestUtils.setField(organizationController, "service", organizationService);
        ReflectionTestUtils.setField(roleController, "service", roleService);
        mvc = MockMvcBuilders
                .standaloneSetup(
                        tenantController,
                        organizationController,
                        roleController
                )
                .setControllerAdvice(new IamWebExceptionHandler())
                .addFilters(new CurrentUserWebFilter(() -> java.util.Optional.ofNullable(currentUser)))
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        CurrentUserContext.clear();
    }

    @Test
    void shouldQueryAndCreateTenantThroughSystemManagedWebContract() throws Exception {
        when(tenantDao.pageQuery(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(tenant("tenant_a", "Tenant A")), 1, PageRequest.of(1, 20)));
        when(tenantDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(tenant("tenant_b", "Tenant B")));
        when(tenantDao.insert(any())).thenAnswer(invocation -> {
            assertThat(TenantContext.isSystem()).isTrue();
            return invocation.<Tenant>getArgument(0).getAlias();
        });

        mvc.perform(post("/iam.tenant/query"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].alias").value("tenant_a"))
                .andExpect(jsonPath("$.records[0].title").value("Tenant A"))
                .andExpect(jsonPath("$.pageNum").value(1))
                .andExpect(jsonPath("$.pageSize").value(20));

        mvc.perform(post("/iam.tenant/insert")
                        .contentType("application/json")
                        .content(json(tenant("tenant_b", "Tenant B"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alias").value("tenant_b"));
    }

    @Test
    void shouldRejectUnsupportedStaticQueryConditionsInsteadOfIgnoringThem() throws Exception {
        mvc.perform(post("/iam.tenant/query")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "conditions", List.of(Map.of(
                                        "fieldName", "title",
                                        "operator", "EQ",
                                        "values", List.of("Tenant A")
                                ))
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IAM_BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("query conditions are not supported by iam.tenant"));
    }

    @Test
    void shouldExposeOrganizationTreeUnderTenantScope() throws Exception {
        currentUser = CurrentUser.tenantUser("user-1", "User", "tenant_a");
        when(tenantDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(tenant("tenant_a", "Tenant A")));
        when(organizationDao.query(any(Criteria.class), any(PageRequest.class), any()))
                .thenAnswer(invocation -> {
                    assertThat(TenantContext.currentTenantId()).contains("tenant_a");
                    Organization organization = organization("org-1", "HQ", "Headquarters");
                    organization.setTenantId("tenant_a");
                    organization.setParentId(TreeAbility.ROOT_ID);
                    return List.of(organization);
                })
                .thenReturn(List.of());

        mvc.perform(get("/iam.organization/tree?flat=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].id").value("org-1"))
                .andExpect(jsonPath("$.records[0].tenantId").value("tenant_a"))
                .andExpect(jsonPath("$.records[0].code").value("HQ"));
    }

    @Test
    void shouldExposeOrganizationNestedTreeByDefault() throws Exception {
        currentUser = CurrentUser.tenantUser("user-1", "User", "tenant_a");
        when(tenantDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(tenant("tenant_a", "Tenant A")));
        Organization root = organization("org-1", "HQ", "Headquarters");
        root.setTenantId("tenant_a");
        root.setParentId(TreeAbility.ROOT_ID);
        Organization child = organization("org-2", "BR", "Branch");
        child.setTenantId("tenant_a");
        child.setParentId("org-1");
        when(organizationDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(root), List.of(child));
        when(organizationDao.query(any(Criteria.class), any(PageRequest.class), any()))
                .thenReturn(List.of(root), List.of(child), List.of());

        mvc.perform(get("/iam.organization/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].record.id").value("org-1"))
                .andExpect(jsonPath("$.records[0].record.tenantId").value("tenant_a"))
                .andExpect(jsonPath("$.records[0].children[0].record.id").value("org-2"))
                .andExpect(jsonPath("$.records[0].children[0].children").isArray());
    }

    @Test
    void shouldCreateOrganizationUnderTenantScope() throws Exception {
        currentUser = CurrentUser.tenantUser("user-1", "User", "tenant_a");
        when(tenantDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(tenant("tenant_a", "Tenant A")));
        when(organizationDao.insert(any())).thenAnswer(invocation -> {
            assertThat(TenantContext.currentTenantId()).contains("tenant_a");
            Organization organization = invocation.getArgument(0);
            assertThat(organization.getTenantId()).isEqualTo("tenant_a");
            return "org-1";
        });
        when(organizationDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(organization("org-1", "HQ", "Headquarters")));

        mvc.perform(post("/iam.organization/insert")
                        .contentType("application/json")
                        .content(json(organization(null, "HQ", "Headquarters"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("org-1"));
    }

    @Test
    void shouldRejectOrganizationAccessWhenTenantIsInactive() throws Exception {
        currentUser = CurrentUser.tenantUser("user-1", "User", "tenant_a");
        doThrow(new PlatformException("Tenant is not active: tenant_a"))
                .when(tenantDao).query(any(Criteria.class), any(PageRequest.class));

        mvc.perform(get("/iam.organization/tree"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IAM_BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Tenant is not active: tenant_a"));
    }

    @Test
    void shouldRequireCurrentUserTenantForOrganizationAccess() throws Exception {
        mvc.perform(get("/iam.organization/tree"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IAM_BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("iam.organization requires tenant context"));
    }

    @Test
    void shouldDisableTenantThroughSystemContext() throws Exception {
        Tenant existing = tenant("tenant_a", "Tenant A");
        existing.setVersion(2);
        when(tenantDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(existing));
        when(tenantDao.updateByIdAndVersion(any(Tenant.class), any())).thenAnswer(invocation -> {
            assertThat(TenantContext.isSystem()).isTrue();
            return 1;
        });

        mvc.perform(post("/iam.tenant/disable/{tenantAlias}", "tenant_a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        verify(tenantDao).updateByIdAndVersion(any(Tenant.class), any());
    }

    @Test
    void shouldExposeRoleUserBindingAndActionGrantEndpoints() throws Exception {
        currentUser = CurrentUser.tenantUser("user-1", "User", "tenant_a");
        when(roleService.bindUsers("role-1", List.of("user-2", "user-3"))).thenReturn(2);
        when(roleService.userIds("role-1")).thenReturn(List.of("user-2", "user-3"));
        when(roleService.grantAction("role-1", "sales.contract", "query",
                DataScopePolicy.OWNER, TenantScopePolicy.CURRENT_TENANT,
                null, null, null)).thenReturn(1);
        when(roleService.grantWildcardDataScopeAction("scope-1", "query",
                DataScopePolicy.OWNER, TenantScopePolicy.CURRENT_TENANT)).thenReturn(1);
        when(roleService.revokeAction("role-1", "sales.contract", "query")).thenReturn(1);

        mvc.perform(post("/iam.role/users/{roleId}/bind", "role-1")
                        .contentType("application/json")
                        .content("""
                                {"userIds":["user-2","user-3"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
        mvc.perform(get("/iam.role/users/{roleId}", "role-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("user-2"))
                .andExpect(jsonPath("$[1]").value("user-3"));
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
        mvc.perform(post("/iam.role/revoke/{roleId}", "role-1")
                        .contentType("application/json")
                        .content("""
                                {"moduleAlias":"sales.contract","actionCode":"query"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
        mvc.perform(post("/iam.role/wildcard-data-scope/{roleId}/grant", "scope-1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "actionCode":"query",
                                  "dataScopePolicy":"OWNER",
                                  "tenantScopePolicy":"CURRENT_TENANT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void shouldExposeRolePermissionMatrixFromModuleAliases() throws Exception {
        currentUser = CurrentUser.tenantUser("user-1", "User", "tenant_a");
        List<GrantableAction> grantableActions = List.of(
                new GrantableAction("sales.contract", "query", "view", "Query", true, true));
        when(grantableActionResolver.resolve(List.of("sales.contract"))).thenReturn(grantableActions);
        when(roleService.permissionMatrix("role-1", grantableActions)).thenReturn(new RolePermissionMatrix(
                "role-1",
                List.of(new RolePermissionMatrix.Module(
                        "sales.contract",
                        List.of(new net.ximatai.muyun.spring.iam.role.RolePermissionAction(
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
                .andExpect(jsonPath("$.modules[0].actions[0].actionCode").value("query"))
                .andExpect(jsonPath("$.modules[0].actions[0].permissionActionCode").value("view"))
                .andExpect(jsonPath("$.modules[0].actions[0].granted").value(true));
    }

    private Tenant tenant(String alias, String title) {
        Tenant tenant = new Tenant();
        tenant.setAlias(alias);
        tenant.setTitle(title);
        tenant.setEnabled(Boolean.TRUE);
        tenant.setSortOrder(1);
        return tenant;
    }

    private Organization organization(String id, String code, String title) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setCode(code);
        organization.setTitle(title);
        organization.setEnabled(Boolean.TRUE);
        organization.setSortOrder(1);
        return organization;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
