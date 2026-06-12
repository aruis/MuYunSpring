package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;

public abstract class TenantStandardBusinessService<T extends EntityContract> extends TenantActiveScopedService<T> {
    protected TenantStandardBusinessService(
            String moduleAlias,
            Class<T> modelClass,
            BaseDao<T, String> dao,
            ActiveTenantVerifier activeTenantVerifier
    ) {
        super(moduleAlias, modelClass, dao, activeTenantVerifier);
    }

    @Override
    public void beforeInsert(T entity) {
        validateBeforeSave(entity);
        validateBeforeInsert(entity);
    }

    @Override
    public void beforeUpdate(T entity) {
        requireActiveTenantMutationContext();
        normalizeBeforeMutation(entity);
        validateBeforeSave(entity);
        validateBeforeUpdate(entity);
    }

    protected void validateBeforeSave(T entity) {
    }

    protected void validateBeforeInsert(T entity) {
    }

    protected void validateBeforeUpdate(T entity) {
    }
}
