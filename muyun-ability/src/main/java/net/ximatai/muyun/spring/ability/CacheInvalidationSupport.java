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
            cacheAbility.clearItemCache(entity.getId());
        }
    }
}
