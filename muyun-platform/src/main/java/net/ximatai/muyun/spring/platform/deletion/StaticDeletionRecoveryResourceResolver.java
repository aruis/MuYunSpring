package net.ximatai.muyun.spring.platform.deletion;

import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.deletion.DeletionRecoveryAbility;
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
    private final Map<ResourceKey, SoftDeleteAbility<?>> abilities;

    public StaticDeletionRecoveryResourceResolver(Collection<DeletionRecoveryAbility<?>> abilities) {
        Map<ResourceKey, SoftDeleteAbility<?>> indexed = new LinkedHashMap<>();
        for (DeletionRecoveryAbility<?> ability : abilities == null ? List.<DeletionRecoveryAbility<?>>of() : abilities) {
            if (ability == null || blank(ability.getModuleAlias()) || blank(ability.getDeletionEntityAlias())) {
                continue;
            }
            ResourceKey key = new ResourceKey(ability.getModuleAlias(), ability.getDeletionEntityAlias());
            SoftDeleteAbility<?> previous = indexed.putIfAbsent(key, ability);
            if (previous != null && previous != ability) {
                throw new IllegalArgumentException("Duplicate static deletion recovery ability for " + key + ": "
                        + previous.getClass().getName() + ", "
                        + ability.getClass().getName());
            }
        }
        this.abilities = Map.copyOf(indexed);
    }

    @Override
    public boolean supports(DeletionEntry entry) {
        Objects.requireNonNull(entry, "deletionEntry must not be null");
        return abilities.containsKey(ResourceKey.from(entry));
    }

    @Override
    public Optional<SoftDeleteAbility<?>> resolve(DeletionEntry entry) {
        Objects.requireNonNull(entry, "deletionEntry must not be null");
        return Optional.ofNullable(abilities.get(ResourceKey.from(entry)));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private record ResourceKey(String moduleAlias, String entityAlias) {
        private static ResourceKey from(DeletionEntry entry) {
            return new ResourceKey(entry.getResourceModuleAlias(), entry.getResourceEntityAlias());
        }
    }
}
