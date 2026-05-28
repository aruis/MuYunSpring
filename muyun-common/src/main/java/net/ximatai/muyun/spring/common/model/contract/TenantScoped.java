package net.ximatai.muyun.spring.common.model.contract;

/**
 * Marks records that belong to a tenant boundary.
 */
public interface TenantScoped {
    String getTenantId();

    void setTenantId(String tenantId);
}
