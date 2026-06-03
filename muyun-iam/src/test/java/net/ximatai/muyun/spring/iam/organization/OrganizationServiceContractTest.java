package net.ximatai.muyun.spring.iam.organization;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.iam.tenant.Tenant;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.ability.TreeAbility;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrganizationServiceContractTest {
    @Test
    void shouldExposeStableModuleAlias() {
        OrganizationService service = new OrganizationService(mock(OrganizationDao.class));

        assertThat(service.getModuleAlias()).isEqualTo("iam.organization");
    }

    @Test
    void shouldFillOrganizationDefaultsThroughCrudAbility() {
        OrganizationDao dao = mock(OrganizationDao.class);
        when(dao.insert(any())).thenReturn("org-1");
        TenantService tenantService = activeTenantService("tenant_a");
        OrganizationService service = new OrganizationService(dao, tenantService);
        Organization organization = organization("HQ", "Headquarters");

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            service.insert(organization);
        }

        assertThat(organization.getEnabled()).isTrue();
        assertThat(organization.getParentId()).isEqualTo(TreeAbility.ROOT_ID);
        assertThat(organization.getTenantId()).isEqualTo("tenant_a");
        verify(tenantService).requireActiveTenant("tenant_a");
    }

    @Test
    void shouldRequireTenantContextForOrganizationMutation() {
        OrganizationService service = new OrganizationService(mock(OrganizationDao.class), activeTenantService("tenant_a"));

        assertThatThrownBy(() -> service.insert(organization("HQ", "Headquarters")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("tenant context");

        try (TenantContext.Scope ignored = TenantContext.system()) {
            assertThatThrownBy(() -> service.insert(organization("HQ", "Headquarters")))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("tenant context");
        }
    }

    @Test
    void shouldRejectInactiveTenantForOrganizationMutation() {
        TenantService tenantService = mock(TenantService.class);
        when(tenantService.requireActiveTenant("tenant_a"))
                .thenThrow(new PlatformException("Tenant is not active: tenant_a"));
        OrganizationService service = new OrganizationService(mock(OrganizationDao.class), tenantService);

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
        OrganizationService service = new OrganizationService(dao, activeTenantService("tenant_a"));

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            Organization branch = organization("BR-001", "Branch");
            service.insert(branch);
            assertThat(branch.getCode()).isEqualTo("BR-001");

            assertThatThrownBy(() -> service.insert(organization(" ", "Blank Code")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("organizationCode");
        }
    }

    private Organization organization(String code, String title) {
        Organization organization = new Organization();
        organization.setCode(code);
        organization.setTitle(title);
        return organization;
    }

    private TenantService activeTenantService(String tenantAlias) {
        TenantService tenantService = mock(TenantService.class);
        Tenant tenant = new Tenant();
        tenant.setAlias(tenantAlias);
        tenant.setTitle(tenantAlias);
        tenant.setEnabled(Boolean.TRUE);
        when(tenantService.requireActiveTenant(tenantAlias)).thenReturn(tenant);
        return tenantService;
    }
}
