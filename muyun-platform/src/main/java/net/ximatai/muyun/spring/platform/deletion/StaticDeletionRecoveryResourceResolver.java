package net.ximatai.muyun.spring.platform.deletion;

import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Resolves statically declared resource services by their stable module alias. */
@Component
public class StaticDeletionRecoveryResourceResolver implements DeletionRecoveryResourceResolver {
    private final Map<String, SoftDeleteAbility<?>> abilities;

    public StaticDeletionRecoveryResourceResolver(Collection<SoftDeleteAbility<?>> abilities) {
        Map<String, SoftDeleteAbility<?>> indexed = new LinkedHashMap<>();
        for (SoftDeleteAbility<?> ability : abilities == null ? List.<SoftDeleteAbility<?>>of() : abilities) {
            if (ability == null || ability.getModuleAlias() == null || ability.getModuleAlias().isBlank()) {
                continue;
            }
            SoftDeleteAbility<?> previous = indexed.putIfAbsent(ability.getModuleAlias(), ability);
            if (previous != null && previous != ability) {
                throw new IllegalArgumentException("Duplicate static deletion recovery ability for module "
                        + ability.getModuleAlias() + ": " + previous.getClass().getName() + ", "
                        + ability.getClass().getName());
            }
        }
        this.abilities = Map.copyOf(indexed);
    }

    @Override
    public Optional<SoftDeleteAbility<?>> resolve(DeletionEntry entry) {
        Objects.requireNonNull(entry, "deletionEntry must not be null");
        return Optional.ofNullable(abilities.get(entry.getResourceModuleAlias()));
    }
}
