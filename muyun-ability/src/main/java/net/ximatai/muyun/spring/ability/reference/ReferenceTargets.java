package net.ximatai.muyun.spring.ability.reference;

import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;

/** Resolves the stable reference identity of static and dynamic abilities. */
public final class ReferenceTargets {
    private ReferenceTargets() {
    }

    public static ReferenceTarget of(CrudAbility<?> ability) {
        if (ability instanceof ReferenceTargetProvider provider) {
            return provider.referenceTarget();
        }
        return fromModuleAlias(ability == null ? null : ability.getModuleAlias());
    }

    public static ReferenceTarget fromModuleAlias(String moduleAlias) {
        int separator = moduleAlias == null ? -1 : moduleAlias.lastIndexOf('.');
        if (separator <= 0 || separator == moduleAlias.length() - 1) {
            throw new PlatformException("reference target requires '<applicationAlias>.<entityAlias>': " + moduleAlias);
        }
        return ReferenceTarget.of(moduleAlias.substring(0, separator), moduleAlias.substring(separator + 1));
    }
}
