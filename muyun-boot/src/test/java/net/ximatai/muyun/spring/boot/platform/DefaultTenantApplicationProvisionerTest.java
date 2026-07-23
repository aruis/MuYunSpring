package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.iam.tenant.TenantApplicationService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DefaultTenantApplicationProvisionerTest {
    @Test
    void shouldEnsureIamApplicationWhenTenantIsCreatedOrProvisionedAgain() {
        TenantApplicationService tenantApplicationService = mock(TenantApplicationService.class);
        DefaultTenantApplicationProvisioner provisioner = new DefaultTenantApplicationProvisioner(tenantApplicationService);

        provisioner.afterTenantCreated("tenant_a");

        verify(tenantApplicationService).ensureRequiredApplications("tenant_a");
    }
}
