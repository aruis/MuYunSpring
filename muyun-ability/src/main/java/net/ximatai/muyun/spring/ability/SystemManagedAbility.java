package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.tenant.TenantContext;

public interface SystemManagedAbility<T extends EntityContract> extends CrudAbility<T> {
    @Override
    default void beforePrepareInsert(T entity) {
        requireSystemMutationContext();
        normalizeBeforeMutation(entity);
    }

    @Override
    default void beforeUpdate(T entity) {
        requireSystemMutationContext();
        normalizeBeforeMutation(entity);
    }

    @Override
    default void beforeDelete(String id) {
        requireSystemMutationContext();
    }

    default void normalizeBeforeMutation(T entity) {
    }

    default void requireSystemMutationContext() {
        if (!TenantContext.isSystem()) {
            throw new PlatformException(getModuleAlias() + " management requires system context");
        }
    }
}
