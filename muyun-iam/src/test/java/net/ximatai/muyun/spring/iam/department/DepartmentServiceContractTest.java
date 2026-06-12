package net.ximatai.muyun.spring.iam.department;

import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.iam.organization.Organization;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DepartmentServiceContractTest {
    @Test
    void shouldExposeStableModuleAlias() {
        DepartmentService service = new DepartmentService(mock(DepartmentDao.class), activeTenantVerifier(),
                organizationService());

        assertThat(service.getModuleAlias()).isEqualTo("iam.department");
    }

    @Test
    void shouldFillDepartmentDefaultsThroughCrudAbility() {
        DepartmentDao dao = mock(DepartmentDao.class);
        when(dao.insert(any())).thenReturn("dept-1");
        ActiveTenantVerifier tenantVerifier = activeTenantVerifier();
        OrganizationService organizationService = organizationService();
        when(organizationService.requireEnabled(eq("org-1"), any())).thenReturn(organization("org-1"));
        DepartmentService service = new DepartmentService(dao, tenantVerifier, organizationService);
        Department department = department("org-1", "FIN", "Finance");

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            service.insert(department);
        }

        assertThat(department.getEnabled()).isTrue();
        assertThat(department.getParentId()).isEqualTo(TreeAbility.ROOT_ID);
        assertThat(department.getTenantId()).isEqualTo("tenant_a");
        verify(tenantVerifier).verifyActiveTenant("tenant_a");
        verify(organizationService).requireEnabled(eq("org-1"), any());
    }

    @Test
    void shouldRequireTenantContextForDepartmentMutation() {
        DepartmentService service = new DepartmentService(mock(DepartmentDao.class), activeTenantVerifier(),
                organizationService());

        assertThatThrownBy(() -> service.insert(department("org-1", "FIN", "Finance")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("tenant context");

        try (TenantContext.Scope ignored = TenantContext.system("test system context")) {
            assertThatThrownBy(() -> service.insert(department("org-1", "FIN", "Finance")))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("tenant context");
        }
    }

    @Test
    void shouldRejectInactiveTenantForDepartmentMutation() {
        ActiveTenantVerifier tenantVerifier = activeTenantVerifier();
        doThrow(new PlatformException("Tenant is not active: tenant_a"))
                .when(tenantVerifier).verifyActiveTenant("tenant_a");
        DepartmentService service = new DepartmentService(mock(DepartmentDao.class), tenantVerifier,
                organizationService());

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThatThrownBy(() -> service.insert(department("org-1", "FIN", "Finance")))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("not active");
        }
    }

    @Test
    void shouldRequireOrganizationAndDepartmentCode() {
        DepartmentDao dao = mock(DepartmentDao.class);
        when(dao.insert(any())).thenReturn("dept-1");
        OrganizationService organizationService = organizationService();
        when(organizationService.requireEnabled(eq("org-1"), any())).thenReturn(organization("org-1"));
        DepartmentService service = new DepartmentService(dao, activeTenantVerifier(), organizationService);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            Department finance = department("org-1", "FIN-001", "Finance");
            service.insert(finance);
            assertThat(finance.getOrganizationId()).isEqualTo("org-1");
            assertThat(finance.getCode()).isEqualTo("FIN-001");

            assertThatThrownBy(() -> service.insert(department(" ", "FIN", "Blank Organization")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("organizationId");
            assertThatThrownBy(() -> service.insert(department("org-1", " ", "Blank Code")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("departmentCode");
        }
    }

    @Test
    void shouldRejectMissingOrDisabledOrganization() {
        OrganizationService organizationService = organizationService();
        when(organizationService.requireEnabled(eq("org-missing"), any()))
                .thenThrow(new PlatformException("organization is not active: org-missing"));
        DepartmentService service = new DepartmentService(mock(DepartmentDao.class), activeTenantVerifier(),
                organizationService);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThatThrownBy(() -> service.insert(department("org-missing", "FIN", "Finance")))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("organization is not active");
        }
    }

    @Test
    void shouldPreserveTenantAndNormalizeChecksBeforeUpdate() {
        OrganizationService organizationService = organizationService();
        when(organizationService.requireEnabled(eq("org-1"), any())).thenReturn(organization("org-1"));
        ActiveTenantVerifier tenantVerifier = activeTenantVerifier();
        DepartmentService service = new DepartmentService(mock(DepartmentDao.class), tenantVerifier,
                organizationService);

        assertThatThrownBy(() -> service.beforeUpdate(department("org-1", "FIN", "Finance")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("tenant context");

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThatThrownBy(() -> service.beforeUpdate(department("org-1", " ", "Blank Code")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("departmentCode");
        }

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            service.beforeUpdate(department("org-1", "FIN", "Finance"));
        }
        verify(tenantVerifier, times(2)).verifyActiveTenant("tenant_a");
        verify(organizationService).requireEnabled(eq("org-1"), any());
    }

    @Test
    void shouldRejectParentFromAnotherOrganization() {
        DepartmentService service = spy(new DepartmentService(mock(DepartmentDao.class), activeTenantVerifier(),
                organizationService()));
        Department parent = department("org-1", "FIN", "Finance");
        parent.setId("dept-parent");
        Department child = department("org-2", "AR", "Accounts Receivable");
        child.setId("dept-child");
        child.setParentId("dept-parent");
        doReturn(parent).when(service).select("dept-parent");

        assertThatThrownBy(() -> service.validateTreePlacement(child))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("same organization");
    }

    @Test
    void shouldRejectSortAcrossOrganizations() {
        DepartmentService service = new DepartmentService(mock(DepartmentDao.class), activeTenantVerifier(),
                organizationService());

        assertThatThrownBy(() -> service.validateSortScope(
                department("org-1", "FIN", "Finance"),
                department("org-2", "FIN", "Finance")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("same organization");
    }

    @Test
    void shouldResolveOrganizationScopedChildren() {
        DepartmentService service = spy(new DepartmentService(mock(DepartmentDao.class), activeTenantVerifier(),
                organizationService()));
        Department root = department("org-1", "FIN", "Finance");
        doReturn(List.of(root)).when(service).children(any(), eq(TreeAbility.ROOT_ID));

        assertThat(service.rootDepartments("org-1")).containsExactly(root);
    }

    private Department department(String organizationId, String code, String title) {
        Department department = new Department();
        department.setOrganizationId(organizationId);
        department.setCode(code);
        department.setTitle(title);
        return department;
    }

    private ActiveTenantVerifier activeTenantVerifier() {
        return mock(ActiveTenantVerifier.class);
    }

    private OrganizationService organizationService() {
        return mock(OrganizationService.class);
    }

    private Organization organization(String id) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setEnabled(Boolean.TRUE);
        return organization;
    }
}
