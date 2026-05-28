package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.ability.reference.ReferencerAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

final class CacheInvalidationSupport {
    private CacheInvalidationSupport() {
    }

    static void clearAfterChanged(Object ability, EntityContract entity) {
        TransactionScopeSupport.afterCommitOrNow(() -> clearAfterChangedNow(ability, entity));
    }

    private static void clearAfterChangedNow(Object ability, EntityContract entity) {
        if (ability instanceof CacheAbility<?> cacheAbility) {
            if (entity == null || entity.getId() == null) {
                cacheAbility.clearCache();
                return;
            }
            if (ability instanceof ReferencerAbility<?> referencerAbility) {
                referencerAbility.clearReferenceDependency(entity.getId());
            }
            cacheAbility.clearItemCache(entity.getId());
        }
        if (ability instanceof ReferenceAbility<?> referenceAbility && entity != null && entity.getId() != null) {
            referenceAbility.clearReferenceReferrers(entity.getId());
        }
    }
}
