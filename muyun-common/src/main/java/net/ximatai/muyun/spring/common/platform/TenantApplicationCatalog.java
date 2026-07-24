package net.ximatai.muyun.spring.common.platform;

/**
 * Platform-owned catalog used to validate which applications can be opened for a tenant.
 *
 * <p>The contract lives in common so IAM can preserve the tenant entitlement invariant without
 * depending on the platform configuration implementation.</p>
 */
public interface TenantApplicationCatalog {
    boolean isEnabledForTenant(String applicationAlias);

    void requireEnabledForTenant(String applicationAlias);
}
