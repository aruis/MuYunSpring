package net.ximatai.muyun.spring.ability.reference;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Executes a fully resolved typed reference path for static and dynamic records alike. */
public final class ReferenceLoadReader {
    private ReferenceLoadReader() {
    }

    public static Object read(ReferenceLoadPath path,
                              List<String> sourceIds,
                              Function<ReferenceTarget, ReferenceAbility<?>> abilities) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return null;
        }
        List<String> ids = sourceIds;
        ReferenceTarget current = path.sourceTarget();
        for (ReferenceLoadPath.Hop hop : path.hops()) {
            if (hop.viaField() == null) {
                throw new IllegalArgumentException("ReferenceLoad path hop must resolve via field: " + current);
            }
            Map<String, Map<String, Object>> values = require(abilities, current).projections(ids, List.of(hop.viaField()));
            ids = ids.stream()
                    .map(values::get)
                    .map(fields -> fields == null ? null : fields.get(hop.viaField()))
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .toList();
            if (ids.isEmpty()) {
                return null;
            }
            current = hop.target();
        }
        String terminalId = ids.getFirst();
        return require(abilities, current).projections(List.of(terminalId), List.of(path.terminalField()))
                .getOrDefault(terminalId, Map.of())
                .get(path.terminalField());
    }

    private static ReferenceAbility<?> require(Function<ReferenceTarget, ReferenceAbility<?>> abilities,
                                               ReferenceTarget target) {
        ReferenceAbility<?> ability = abilities.apply(target);
        if (ability == null) {
            throw new IllegalArgumentException("ReferenceLoad target is unavailable: " + target.qualifiedName());
        }
        return ability;
    }
}
