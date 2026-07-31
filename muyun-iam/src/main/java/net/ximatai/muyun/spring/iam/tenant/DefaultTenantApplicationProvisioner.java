package net.ximatai.muyun.spring.iam.tenant;

import net.ximatai.muyun.spring.common.tenant.TenantCreationProvisioner;
import net.ximatai.muyun.spring.common.util.Preconditions;
import net.ximatai.muyun.spring.iam.tenant.TenantApplicationService;

import java.util.Objects;

/** Opens the mandatory IAM application whenever a tenant is created or provisioned again. */
public class DefaultTenantApplicationProvisioner implements TenantCreationProvisioner {
    private final TenantApplicationService tenantApplicationService;

    public DefaultTenantApplicationProvisioner(TenantApplicationService tenantApplicationService) {
        this.tenantApplicationService = Objects.requireNonNull(tenantApplicationService,
                "tenantApplicationService must not be null");
    }

    @Override
    public void afterTenantCreated(String tenantId) {
        tenantApplicationService.ensureRequiredApplications(Preconditions.requireText(tenantId, "tenantId"));
    }
}
