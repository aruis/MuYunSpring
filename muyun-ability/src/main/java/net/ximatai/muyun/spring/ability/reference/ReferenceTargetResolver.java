package net.ximatai.muyun.spring.ability.reference;

import java.util.Optional;

/** Resolves a reference target without exposing whether it is static or dynamic. */
@FunctionalInterface
public interface ReferenceTargetResolver {
    ReferenceTargetResolver NONE = target -> Optional.empty();

    Optional<ReferenceAbility<?>> resolve(ReferenceTarget target);
}
