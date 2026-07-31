package net.ximatai.muyun.spring.ability.child;

import java.util.Optional;

/** Resolves the runtime child ability for a declared static child relation. */
@FunctionalInterface
public interface ChildAbilityResolver {
    ChildAbilityResolver NONE = child -> Optional.empty();

    Optional<ChildAbility<?>> resolve(ChildAbilityRequest child);
}
