package net.ximatai.muyun.spring.boot.reference;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceDeletionGuard;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.ability.reference.ReferenceTargetDeletionPolicy;
import net.ximatai.muyun.spring.ability.reference.ReferenceTargetProvider;
import net.ximatai.muyun.spring.ability.reference.StaticReferenceResolver;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

import java.util.List;

/** Checks active static referrers before a target using RESTRICT is soft-deleted. */
public final class StaticReferenceDeletionGuard implements ReferenceDeletionGuard {
    private final List<CrudAbility<?>> abilities;

    public StaticReferenceDeletionGuard(List<CrudAbility<?>> abilities) {
        this.abilities = abilities == null ? List.of() : List.copyOf(abilities);
    }

    @Override
    public void beforeSoftDelete(CrudAbility<?> targetAbility, EntityContract target) {
        if (targetAbility == null || target == null || target.getId() == null || target.getId().isBlank()) {
            return;
        }
        ReferenceTarget targetReference = targetOf(targetAbility);
        for (CrudAbility<?> sourceAbility : abilities) {
            Class<?> modelClass = sourceAbility.modelClass();
            if (modelClass == null) {
                continue;
            }
            for (StaticReferenceResolver.ReferenceRule rule : StaticReferenceResolver.rules(modelClass)) {
                if (!targetReference.equals(rule.target())
                        || rule.integrity().onTargetSoftDelete() != ReferenceTargetDeletionPolicy.RESTRICT) {
                    continue;
                }
                long count = sourceAbility.count(Criteria.of().eq(rule.plan().sourceField(), target.getId()));
                if (count > 0) {
                    throw new PlatformException("cannot soft-delete reference target " + targetReference.qualifiedName()
                            + ": " + count + " active records in " + sourceAbility.getModuleAlias()
                            + "." + rule.plan().sourceField() + " still reference it");
                }
            }
        }
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
