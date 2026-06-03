package net.ximatai.muyun.spring.boot.iam;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.boot.web.TenantContextWebFilter;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.iam.organization.Organization;
import net.ximatai.muyun.spring.iam.organization.OrganizationDao;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IamWebControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private TenantDao tenantDao;
    private OrganizationDao organizationDao;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        tenantDao = mock(TenantDao.class);
        organizationDao = mock(OrganizationDao.class);
        TenantService tenantService = new TenantService(tenantDao);
        OrganizationService organizationService = new OrganizationService(organizationDao, tenantService);
        TenantWebController tenantController = new TenantWebController();
        OrganizationWebController organizationController = new OrganizationWebController();
        ReflectionTestUtils.setField(tenantController, "service", tenantService);
        ReflectionTestUtils.setField(organizationController, "service", organizationService);
        mvc = MockMvcBuilders
                .standaloneSetup(
                        tenantController,
                        organizationController
                )
                .setControllerAdvice(new IamWebExceptionHandler())
                .addFilters(new TenantContextWebFilter())
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
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
    void shouldExposeOrganizationTreeUnderTenantScope() throws Exception {
        when(tenantDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(tenant("tenant_a", "Tenant A")));
        when(organizationDao.query(any(Criteria.class), any(PageRequest.class), any()))
                .thenAnswer(invocation -> {
                    assertThat(TenantContext.currentTenantId()).contains("tenant_a");
                    Organization organization = organization("org-1", "HQ", "Headquarters");
                    organization.setTenantId("tenant_a");
                    organization.setParentId(TreeAbility.ROOT_ID);
                    return List.of(organization);
                });

        mvc.perform(post("/iam.organization/tree")
                        .header(TenantContextWebFilter.TENANT_HEADER, "tenant_a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].id").value("org-1"))
                .andExpect(jsonPath("$.records[0].tenantId").value("tenant_a"))
                .andExpect(jsonPath("$.records[0].code").value("HQ"));
    }

    @Test
    void shouldCreateOrganizationUnderTenantScope() throws Exception {
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
                        .header(TenantContextWebFilter.TENANT_HEADER, "tenant_a")
                        .contentType("application/json")
                        .content(json(organization(null, "HQ", "Headquarters"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("org-1"));
    }

    @Test
    void shouldRejectOrganizationAccessWhenTenantIsInactive() throws Exception {
        doThrow(new PlatformException("Tenant is not active: tenant_a"))
                .when(tenantDao).query(any(Criteria.class), any(PageRequest.class));

        mvc.perform(post("/iam.organization/tree")
                        .header(TenantContextWebFilter.TENANT_HEADER, "tenant_a"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IAM_BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Tenant is not active: tenant_a"));
    }

    @Test
    void shouldRequireTenantHeaderForOrganizationAccess() throws Exception {
        mvc.perform(post("/iam.organization/tree"))
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
