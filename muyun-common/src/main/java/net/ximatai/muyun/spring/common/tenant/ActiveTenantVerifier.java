package net.ximatai.muyun.spring.common.tenant;

public interface ActiveTenantVerifier {
    void verifyActiveTenant(String tenantId);
}
