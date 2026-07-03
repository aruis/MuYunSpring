package net.ximatai.muyun.spring.iam.organization;

import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaSqlCompiler;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.ability.TreeAbility;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class OrganizationServiceContractTest {
    @Test
    void shouldExposeStableModuleAlias() {
        OrganizationService service = new OrganizationService(mock(OrganizationDao.class), activeTenantVerifier());

        assertThat(service.getModuleAlias()).isEqualTo("iam.organization");
    }

    @Test
    void shouldFillOrganizationDefaultsThroughCrudAbility() {
        OrganizationDao dao = mock(OrganizationDao.class);
        when(dao.insert(any())).thenReturn("org-1");
        ActiveTenantVerifier tenantVerifier = activeTenantVerifier();
        OrganizationService service = new OrganizationService(dao, tenantVerifier);
        Organization organization = organization("HQ", "Headquarters");

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            service.insert(organization);
        }

        assertThat(organization.getEnabled()).isTrue();
        assertThat(organization.getParentId()).isEqualTo(TreeAbility.ROOT_ID);
        assertThat(organization.getTenantId()).isEqualTo("tenant_a");
        verify(tenantVerifier).verifyActiveTenant("tenant_a");
    }

    @Test
    void shouldRequireTenantContextForOrganizationMutation() {
        OrganizationService service = new OrganizationService(mock(OrganizationDao.class), activeTenantVerifier());

        assertThatThrownBy(() -> service.insert(organization("HQ", "Headquarters")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("tenant context");

        try (TenantContext.Scope ignored = TenantContext.system("test system context")) {
            assertThatThrownBy(() -> service.insert(organization("HQ", "Headquarters")))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("tenant context");
        }
    }

    @Test
    void shouldRejectInactiveTenantForOrganizationMutation() {
        ActiveTenantVerifier tenantVerifier = activeTenantVerifier();
        doThrow(new PlatformException("Tenant is not active: tenant_a"))
                .when(tenantVerifier).verifyActiveTenant("tenant_a");
        OrganizationService service = new OrganizationService(mock(OrganizationDao.class), tenantVerifier);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThatThrownBy(() -> service.insert(organization("HQ", "Headquarters")))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("not active");
        }
    }

    @Test
    void shouldRequireOrganizationCodeButAllowBusinessCodeShape() {
        OrganizationDao dao = mock(OrganizationDao.class);
        when(dao.insert(any())).thenReturn("org-1");
        OrganizationService service = new OrganizationService(dao, activeTenantVerifier());

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            Organization branch = organization("BR-001", "Branch");
            service.insert(branch);
            assertThat(branch.getCode()).isEqualTo("BR-001");

            assertThatThrownBy(() -> service.insert(organization(" ", "Blank Code")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("organizationCode");
        }
    }

    @Test
    void shouldResolveOrganizationIdsFromSelfToRoot() {
        OrganizationService service = spy(new OrganizationService(mock(OrganizationDao.class), activeTenantVerifier()));
        doReturn(List.of("group-1", "dept-1")).when(service).ancestorIdsAndSelf("dept-1");

        assertThat(service.organizationIdsFromSelfToRoot("dept-1"))
                .containsExactly("dept-1", "group-1");
    }

    @Test
    void shouldResolveOrganizationChildrenInsideExplicitTenantScope() {
        OrganizationDao dao = mock(OrganizationDao.class);
        Organization root = organization("HQ", "Headquarters");
        root.setId("org-1");
        root.setTenantId("tenant_a");
        root.setParentId(TreeAbility.ROOT_ID);
        when(dao.query(any(Criteria.class), any(PageRequest.class), any(Sort[].class))).thenReturn(List.of(root));
        OrganizationService service = new OrganizationService(dao, activeTenantVerifier());

        List<Organization> records = service.organizationChildrenForAction(
                PlatformAction.TREE, "tenant_a", TreeAbility.ROOT_ID);

        assertThat(records).containsExactly(root);
        ArgumentCaptor<Criteria> criteriaCaptor = ArgumentCaptor.captor();
        verify(dao).query(criteriaCaptor.capture(), any(PageRequest.class), any(Sort[].class));
        assertThat(compiledCriteria(criteriaCaptor.getValue())).contains("\"tenantId\" =");
        assertThat(compiledCriteria(criteriaCaptor.getValue())).contains("\"parentId\" =");
    }

    @Test
    void shouldMoveOrganizationTreeInsideExplicitTenantContext() {
        OrganizationService service = spy(new OrganizationService(mock(OrganizationDao.class), activeTenantVerifier()));
        doAnswer(invocation -> {
            assertThat(TenantContext.currentTenantId()).contains("tenant_a");
            return null;
        }).when(service).moveInTree(any(Criteria.class), eq("org-1"), eq("org-0"), isNull(), eq(TreeAbility.ROOT_ID));

        try (TenantContext.Scope ignored = TenantContext.system("system organization maintenance")) {
            service.moveInOrganizationTree("tenant_a", "org-1", "org-0", null, TreeAbility.ROOT_ID);
            assertThat(TenantContext.isSystem()).isTrue();
        }
    }

    @Test
    void shouldRequireExplicitTenantScopeForOrganizationTreeServiceEntrypoints() {
        OrganizationService service = new OrganizationService(mock(OrganizationDao.class), activeTenantVerifier());

        assertThatThrownBy(() -> service.organizationChildrenForAction(PlatformAction.TREE, null, TreeAbility.ROOT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
        assertThatThrownBy(() -> service.organizationForAction(PlatformAction.TREE, " ", "org-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
        assertThatThrownBy(() -> service.moveInOrganizationTree(null, "org-1", null, null, TreeAbility.ROOT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
    }

    private Organization organization(String code, String title) {
        Organization organization = new Organization();
        organization.setCode(code);
        organization.setTitle(title);
        return organization;
    }

    private String compiledCriteria(Criteria criteria) {
        return new CriteriaSqlCompiler()
                .compile(criteria, field -> field, DBInfo.Type.POSTGRESQL)
                .getSql();
    }

    private ActiveTenantVerifier activeTenantVerifier() {
        return mock(ActiveTenantVerifier.class);
    }
}
