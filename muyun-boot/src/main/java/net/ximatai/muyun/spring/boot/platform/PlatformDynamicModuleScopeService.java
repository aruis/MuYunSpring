package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.springframework.stereotype.Service;

@Service
public class PlatformDynamicModuleScopeService {
    private final ActiveTenantVerifier activeTenantVerifier;

    public PlatformDynamicModuleScopeService(ActiveTenantVerifier activeTenantVerifier) {
        this.activeTenantVerifier = activeTenantVerifier;
    }

    public void requireTenantScope(String moduleAlias) {
        String tenantId = TenantContext.currentTenantId()
                .orElseThrow(() -> new PlatformException(moduleAlias + " requires tenant context"));
        activeTenantVerifier.verifyActiveTenant(tenantId);
    }
}
