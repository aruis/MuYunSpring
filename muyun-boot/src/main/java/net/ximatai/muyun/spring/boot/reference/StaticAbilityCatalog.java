package net.ximatai.muyun.spring.boot.reference;

import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.ability.reference.ReferenceTargets;
import net.ximatai.muyun.spring.common.exception.PlatformException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** One static-service registry for model, CRUD and reference-target resolution. */
public final class StaticAbilityCatalog {
    private final List<CrudAbility<?>> abilities;
    private final Map<Class<?>, CrudAbility<?>> abilitiesByModel;
    private final Map<ReferenceTarget, CrudAbility<?>> abilitiesByTarget;

    public StaticAbilityCatalog(List<CrudAbility<?>> abilities) {
        LinkedHashMap<Class<?>, CrudAbility<?>> byModel = new LinkedHashMap<>();
        LinkedHashMap<ReferenceTarget, CrudAbility<?>> byTarget = new LinkedHashMap<>();
        for (CrudAbility<?> ability : abilities == null ? List.<CrudAbility<?>>of() : abilities) {
            if (ability == null || ability.modelClass() == null) {
                continue;
            }
            register(byModel, ability.modelClass(), ability, "model");
            register(byTarget, ReferenceTargets.of(ability), ability, "reference target");
        }
        this.abilities = List.copyOf(byModel.values());
        this.abilitiesByModel = Map.copyOf(byModel);
        this.abilitiesByTarget = Map.copyOf(byTarget);
    }

    public List<CrudAbility<?>> abilities() {
        return abilities;
    }

    public Optional<CrudAbility<?>> findByModel(Class<?> modelClass) {
        return Optional.ofNullable(abilitiesByModel.get(modelClass));
    }

    public Optional<CrudAbility<?>> findByTarget(ReferenceTarget target) {
        return Optional.ofNullable(abilitiesByTarget.get(target));
    }

    public Optional<ReferenceAbility<?>> findReference(ReferenceTarget target) {
        return findByTarget(target).filter(ReferenceAbility.class::isInstance).map(ReferenceAbility.class::cast);
    }

    private static <K> void register(Map<K, CrudAbility<?>> index, K key, CrudAbility<?> ability, String kind) {
        CrudAbility<?> existing = index.putIfAbsent(key, ability);
        if (existing != null && existing != ability) {
            String identity = key instanceof ReferenceTarget target ? target.qualifiedName() : String.valueOf(key);
            throw new PlatformException("multiple CRUD services are registered for " + kind + ": " + identity);
        }
    }
}
