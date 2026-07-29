package net.ximatai.muyun.spring.boot.reference;

import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.ability.reference.ReferenceTargetResolver;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordRuntime;

import java.util.List;
import java.util.Optional;

/** Resolves static and dynamic reference targets through one platform boundary. */
public final class PlatformReferenceTargetResolver implements ReferenceTargetResolver {
    private final List<ReferenceAbility<?>> staticAbilities;
    private final DynamicRecordRuntime dynamicRuntime;

    public PlatformReferenceTargetResolver(List<ReferenceAbility<?>> staticAbilities,
                                           DynamicRecordRuntime dynamicRuntime) {
        this.staticAbilities = staticAbilities == null ? List.of() : List.copyOf(staticAbilities);
        this.dynamicRuntime = dynamicRuntime;
    }

    @Override
    public Optional<ReferenceAbility<?>> resolve(ReferenceTarget target) {
        for (ReferenceAbility<?> ability : staticAbilities) {
            if (target.equals(ability.referenceTarget())) {
                return Optional.of(ability);
            }
        }
        return dynamicRuntime == null ? Optional.empty() : dynamicRuntime.referenceAbility(target);
    }
}
