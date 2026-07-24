package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.iam.tenant.Tenant;
import net.ximatai.muyun.spring.iam.tenant.TenantApplicationService;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantApplicationReconciliationTaskTest {
    @Test
    void shouldProvisionMandatoryApplicationsForEveryExistingTenant() {
        Tenant first = new Tenant();
        first.setId("tenant_a");
        Tenant second = new Tenant();
        second.setId("tenant_b");
        TenantService tenantService = mock(TenantService.class);
        TenantApplicationService tenantApplicationService = mock(TenantApplicationService.class);
        when(tenantService.list(any(Criteria.class))).thenReturn(List.of(first, second));

        new TenantApplicationReconciliationTask(tenantService, tenantApplicationService).run();

        verify(tenantApplicationService).ensureRequiredApplications("tenant_a");
        verify(tenantApplicationService).ensureRequiredApplications("tenant_b");
    }
}
