package net.ximatai.muyun.spring.ability.reference;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public record ReferenceLookup(ReferenceTarget target, ReferenceAbility<?> ability) {
    public ReferenceLookup {
        target = Objects.requireNonNull(target, "target must not be null");
        ability = Objects.requireNonNull(ability, "ability must not be null");
    }

    public static ReferenceLookup of(ReferenceAbility<?> ability) {
        Objects.requireNonNull(ability, "ability must not be null");
        return new ReferenceLookup(ability.referenceTarget(), ability);
    }

    Map<String, String> titles(Collection<String> ids) {
        return ability.titles(ids);
    }

    Map<String, Map<String, Object>> projections(Collection<String> ids, Collection<String> sourceFields) {
        return ability.projections(ids, sourceFields);
    }
}
