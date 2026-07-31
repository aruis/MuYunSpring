package net.ximatai.muyun.spring.web;

import net.ximatai.muyun.spring.common.model.contract.EntityContract;

import java.util.Optional;

public interface MutationTenantScopeResolver<T extends EntityContract> {
    /**
     * Resolves the tenant context for system-user mutations of tenant-owned records.
     */
    default Optional<String> tenantIdForCreate(T record) {
        return Optional.empty();
    }

    default Optional<String> tenantIdForUpdate(String id, T record) {
        return tenantIdForExistingRecord(id);
    }

    default Optional<String> tenantIdForExistingRecord(String id) {
        return Optional.empty();
    }
}
