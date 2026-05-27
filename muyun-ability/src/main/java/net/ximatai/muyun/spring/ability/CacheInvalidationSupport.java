package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.model.EntityContract;

final class CacheInvalidationSupport {
    private CacheInvalidationSupport() {
    }

    static void clearAfterChanged(Object ability, EntityContract entity) {
        if (ability instanceof CacheAbility<?> cacheAbility) {
            if (entity == null || entity.getId() == null) {
                cacheAbility.clearCache();
                return;
            }
            ReferenceDependencyRegistry.removeReferrer(cacheAbility.cacheNamespace(), entity.getId());
            cacheAbility.clearItemCache(entity.getId());
        }
        if (ability instanceof ReferenceAbility<?> referenceAbility && entity != null && entity.getId() != null) {
            referenceAbility.clearReferenceReferrers(entity.getId());
        }
    }
}
