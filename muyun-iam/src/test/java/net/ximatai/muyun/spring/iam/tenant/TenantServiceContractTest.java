package net.ximatai.muyun.spring.iam.tenant;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantServiceContractTest {
    @Test
    void shouldCreateTenantInSystemContext() {
        TenantDao dao = mock(TenantDao.class);
        when(dao.insert(any())).thenAnswer(invocation -> invocation.<Tenant>getArgument(0).getId());
        TenantService service = new TenantService(dao);
        Tenant tenant = tenant("ximatai", "Ximatai");
        tenant.setTenantId("should-be-cleared");

        String id;
        try (TenantContext.Scope ignored = TenantContext.system()) {
            id = service.insert(tenant);
        }

        assertThat(id).isEqualTo("ximatai");
        assertThat(tenant.getId()).isEqualTo("ximatai");
        assertThat(tenant.getTenantId()).isNull();
        assertThat(tenant.getEnabled()).isTrue();
    }

    @Test
    void shouldRequireSystemContextForTenantMutation() {
        TenantService service = new TenantService(mock(TenantDao.class));

        assertThatThrownBy(() -> service.insert(tenant("ximatai", "Ximatai")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("system context");

        try (TenantContext.Scope ignored = TenantContext.use("ximatai")) {
            assertThatThrownBy(() -> service.insert(tenant("tenant_b", "Tenant B")))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("system context");
        }
    }

    @Test
    void shouldRejectInvalidTenantAlias() {
        TenantService service = new TenantService(mock(TenantDao.class));

        try (TenantContext.Scope ignored = TenantContext.system()) {
            assertThatThrownBy(() -> service.insert(tenant("tenant-a", "Tenant A")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tenantAlias");
        }
    }

    @Test
    void shouldRequireActiveTenant() {
        TenantDao dao = mock(TenantDao.class);
        when(dao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(tenant("active", "Active")))
                .thenReturn(List.of(disabledTenant("disabled", "Disabled")))
                .thenReturn(List.of());
        TenantService service = new TenantService(dao);

        assertThat(service.requireActiveTenant("active").getTitle()).isEqualTo("Active");
        assertThatThrownBy(() -> service.requireActiveTenant("disabled"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("not active");
        assertThatThrownBy(() -> service.requireActiveTenant("missing"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("not active");
    }

    private Tenant tenant(String alias, String title) {
        Tenant tenant = new Tenant();
        tenant.setAlias(alias);
        tenant.setTitle(title);
        tenant.setEnabled(Boolean.TRUE);
        return tenant;
    }

    private Tenant disabledTenant(String alias, String title) {
        Tenant tenant = tenant(alias, title);
        tenant.setEnabled(Boolean.FALSE);
        return tenant;
    }
}
