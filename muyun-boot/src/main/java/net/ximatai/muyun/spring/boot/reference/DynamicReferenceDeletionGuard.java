package net.ximatai.muyun.spring.boot.reference;

import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceDeletionGuard;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.ability.reference.ReferenceTargetProvider;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordRuntime;

/** Checks dynamic referrers before any static or dynamic target is soft-deleted. */
public final class DynamicReferenceDeletionGuard implements ReferenceDeletionGuard {
    private final DynamicRecordRuntime runtime;

    public DynamicReferenceDeletionGuard(DynamicRecordRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public void beforeSoftDelete(CrudAbility<?> targetAbility, EntityContract target) {
        if (targetAbility == null || target == null || target.getId() == null || target.getId().isBlank()) {
            return;
        }
        runtime.validateReferenceTargetDeletion(targetOf(targetAbility), target.getId());
    }

    private ReferenceTarget targetOf(CrudAbility<?> ability) {
        if (ability instanceof ReferenceTargetProvider provider) {
            return provider.referenceTarget();
        }
        String moduleAlias = ability.getModuleAlias();
        int separator = moduleAlias == null ? -1 : moduleAlias.lastIndexOf('.');
        if (separator <= 0 || separator == moduleAlias.length() - 1) {
            throw new PlatformException("reference target requires '<applicationAlias>.<entityAlias>': " + moduleAlias);
        }
        return ReferenceTarget.of(moduleAlias.substring(0, separator), moduleAlias.substring(separator + 1));
    }
}
