package net.ximatai.muyun.spring.boot.reference;

import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.ability.reference.ReferenceTargetResolver;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordRuntime;

import java.util.Optional;

/** Resolves static and dynamic reference targets through one platform boundary. */
public final class PlatformReferenceTargetResolver implements ReferenceTargetResolver {
    private final StaticAbilityCatalog staticAbilities;
    private final DynamicRecordRuntime dynamicRuntime;

    public PlatformReferenceTargetResolver(StaticAbilityCatalog staticAbilities,
                                           DynamicRecordRuntime dynamicRuntime) {
        this.staticAbilities = staticAbilities;
        this.dynamicRuntime = dynamicRuntime;
    }

    @Override
    public Optional<ReferenceAbility<?>> resolve(ReferenceTarget target) {
        if (staticAbilities != null) {
            Optional<ReferenceAbility<?>> resolved = staticAbilities.findReference(target);
            if (resolved.isPresent()) {
                return resolved;
            }
        }
        return dynamicRuntime == null ? Optional.empty() : dynamicRuntime.referenceAbility(target);
    }
}
