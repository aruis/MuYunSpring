package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.tenant.TenantContext;

public interface TenantActiveScopedAbility<T extends EntityContract> extends CrudAbility<T>, ActiveTenantVerifier {
    @Override
    default void beforePrepareInsert(T entity) {
        requireActiveTenantMutationContext();
        normalizeBeforeMutation(entity);
    }

    @Override
    default void beforeUpdate(T entity) {
        requireActiveTenantMutationContext();
        normalizeBeforeMutation(entity);
    }

    @Override
    default void beforeDelete(String id) {
        requireActiveTenantMutationContext();
    }

    default void normalizeBeforeMutation(T entity) {
    }

    default String requireActiveTenantMutationContext() {
        String tenantId = TenantContext.currentTenantId()
                .orElseThrow(() -> new PlatformException(getModuleAlias() + " management requires tenant context"));
        verifyActiveTenant(tenantId);
        return tenantId;
    }

}
