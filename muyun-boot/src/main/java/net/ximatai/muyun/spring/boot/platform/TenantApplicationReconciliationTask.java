package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.iam.tenant.TenantApplicationService;
import net.ximatai.muyun.spring.iam.tenant.TenantService;

import java.util.Objects;

/** Reconciles mandatory tenant application entitlements for tenants created before this capability existed. */
public class TenantApplicationReconciliationTask implements PlatformBootstrapTask {
    private final TenantService tenantService;
    private final TenantApplicationService tenantApplicationService;

    public TenantApplicationReconciliationTask(TenantService tenantService,
                                                TenantApplicationService tenantApplicationService) {
        this.tenantService = Objects.requireNonNull(tenantService, "tenantService must not be null");
        this.tenantApplicationService = Objects.requireNonNull(
                tenantApplicationService, "tenantApplicationService must not be null");
    }

    @Override
    public String name() {
        return "platform.tenant-application-reconciliation";
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public void run() {
        tenantService.list(Criteria.of())
                .forEach(tenant -> tenantApplicationService.ensureRequiredApplications(tenant.getId()));
    }
}
