package net.ximatai.muyun.spring.ability;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class StaticReferenceResolver {
    private static final Map<Class<?>, List<ReferenceRule>> RULES = new ConcurrentHashMap<>();

    private StaticReferenceResolver() {
    }

    public static Map<ReferenceTarget, Set<String>> collect(Object record) {
        return record == null ? Map.of() : collect(record.getClass(), record);
    }

    public static Map<ReferenceTarget, Set<String>> collect(Class<?> modelClass, Object record) {
        if (modelClass == null || record == null) {
            return Map.of();
        }
        if (!modelClass.isInstance(record)) {
            throw new AbilityException("reference source type mismatch: expected "
                    + modelClass.getName() + ", actual " + record.getClass().getName());
        }
        Map<ReferenceTarget, Set<String>> ids = new LinkedHashMap<>();
        for (ReferenceRule rule : rules(modelClass)) {
            List<String> values = rule.values(record);
            if (values.isEmpty()) {
                continue;
            }
            ids.computeIfAbsent(rule.target(), ignored -> new LinkedHashSet<>()).addAll(values);
        }
        Map<ReferenceTarget, Set<String>> copy = new LinkedHashMap<>();
        ids.forEach((target, values) -> copy.put(target, Collections.unmodifiableSet(new LinkedHashSet<>(values))));
        return Collections.unmodifiableMap(copy);
    }

    public static List<ReferenceRule> rules(Class<?> modelClass) {
        if (modelClass == null) {
            return List.of();
        }
        return RULES.computeIfAbsent(modelClass, StaticReferenceResolver::loadRules);
    }

    public static List<ReferencePlan> plans(Class<?> modelClass) {
        return rules(modelClass).stream()
                .map(ReferenceRule::plan)
                .toList();
    }

    static void clearCacheForTests() {
        RULES.clear();
    }

    private static List<ReferenceRule> loadRules(Class<?> modelClass) {
        LinkedHashMap<String, ReferenceRule> rules = new LinkedHashMap<>();
        Class<?> current = modelClass;
        while (current != null && !Object.class.equals(current)) {
            for (Field field : current.getDeclaredFields()) {
                ReferenceTo referenceTo = field.getAnnotation(ReferenceTo.class);
                if (referenceTo == null) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                } catch (RuntimeException e) {
                    throw new AbilityException("cannot access reference field: "
                            + modelClass.getName() + "." + field.getName(), e);
                }
                rules.putIfAbsent(field.getName(), new ReferenceRule(
                        field,
                        new ReferencePlan(
                                field.getName(),
                                ReferenceTarget.of(referenceTo.moduleAlias(), referenceTo.entityCode()),
                                referenceTo.cardinality(),
                                referenceTo.autoTitle(),
                                referenceTo.titleOutputField()
                        )
                ));
            }
            current = current.getSuperclass();
        }
        return List.copyOf(rules.values());
    }

    public record ReferenceRule(Field field, ReferencePlan plan) {
        public ReferenceTarget target() {
            return plan.target();
        }

        public ReferenceCardinality cardinality() {
            return plan.cardinality();
        }

        private List<String> values(Object record) {
            try {
                return plan.normalizeValues(field.get(record));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot read reference field: " + field.getName(), e);
            }
        }
    }
}
