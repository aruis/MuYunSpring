package net.ximatai.muyun.spring.common.tenant;

public interface TenantCreationProvisioner {
    void afterTenantCreated(String tenantId);
}
