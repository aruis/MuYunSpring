package net.ximatai.muyun.spring.ability.reference;

import java.util.LinkedHashMap;
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
        if (sourceIds.size() != 1) {
            throw new IllegalArgumentException("ReferenceLoad path requires exactly one source id: "
                    + path.sourceField());
        }
        return readAll(path, sourceIds, abilities).get(sourceIds.getFirst());
    }

    /**
     * Resolves one typed path for a batch of source records. Each source id represents one
     * cardinality-ONE path; callers keep the resulting value associated with that source id.
     */
    public static Map<String, Object> readAll(ReferenceLoadPath path,
                                              List<String> sourceIds,
                                              Function<ReferenceTarget, ReferenceAbility<?>> abilities) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return Map.of();
        }
        Map<String, String> currentIds = new LinkedHashMap<>();
        sourceIds.stream().filter(Objects::nonNull).filter(id -> !id.isBlank())
                .forEach(id -> currentIds.putIfAbsent(id, id));
        ReferenceTarget current = path.sourceTarget();
        for (ReferenceLoadPath.Hop hop : path.hops()) {
            if (hop.viaField() == null) {
                throw new IllegalArgumentException("ReferenceLoad path hop must resolve via field: " + current);
            }
            Map<String, Map<String, Object>> values = require(abilities, current).projections(
                    currentIds.values().stream().distinct().toList(), List.of(hop.viaField()));
            currentIds.replaceAll((sourceId, currentId) -> nextId(values, currentId, hop.viaField()));
            currentIds.entrySet().removeIf(entry -> entry.getValue() == null);
            if (currentIds.isEmpty()) {
                return Map.of();
            }
            current = hop.target();
        }
        Map<String, Map<String, Object>> values = require(abilities, current).projections(
                currentIds.values().stream().distinct().toList(), List.of(path.terminalField()));
        Map<String, Object> result = new LinkedHashMap<>();
        currentIds.forEach((sourceId, terminalId) -> result.put(sourceId,
                values.getOrDefault(terminalId, Map.of()).get(path.terminalField())));
        return result;
    }

    private static String nextId(Map<String, Map<String, Object>> values, String currentId, String viaField) {
        Object value = values.getOrDefault(currentId, Map.of()).get(viaField);
        if (value == null) {
            return null;
        }
        String id = String.valueOf(value);
        return id.isBlank() ? null : id;
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
