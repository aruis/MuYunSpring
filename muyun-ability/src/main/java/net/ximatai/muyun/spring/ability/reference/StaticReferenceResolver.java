package net.ximatai.muyun.spring.ability.reference;

import net.ximatai.muyun.spring.common.exception.PlatformException;
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
            throw new PlatformException("reference source type mismatch: expected "
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

    public static List<String> values(Object record, ReferencePlan plan) {
        if (record == null || plan == null) {
            return List.of();
        }
        return rules(record.getClass()).stream()
                .filter(rule -> rule.plan().equals(plan))
                .findFirst()
                .map(rule -> rule.values(record))
                .orElseGet(() -> plan.normalizeValues(readByFieldName(record, plan.sourceField())));
    }

    public static void writeTitleValue(Object record, String titleOutputField, Object titleValue) {
        if (record == null || titleOutputField == null || titleOutputField.isBlank()) {
            return;
        }
        writeByFieldName(record, titleOutputField, titleValue);
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
                    throw new PlatformException("cannot access reference field: "
                            + modelClass.getName() + "." + field.getName(), e);
                }
                rules.putIfAbsent(field.getName(), new ReferenceRule(
                        field,
                        new ReferencePlan(
                                field.getName(),
                                ReferenceTarget.of(referenceTo.moduleAlias(), referenceTo.entityAlias()),
                                referenceTo.cardinality(),
                                referenceTo.autoTitle(),
                                referenceTo.titleOutputField(),
                                projections(referenceTo)
                        )
                ));
            }
            current = current.getSuperclass();
        }
        return List.copyOf(rules.values());
    }

    private static List<ReferenceProjection> projections(ReferenceTo referenceTo) {
        return java.util.Arrays.stream(referenceTo.projections())
                .map(projection -> new ReferenceProjection(projection.targetField(), projection.outputField()))
                .toList();
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

    private static Object readByFieldName(Object record, String fieldName) {
        Class<?> current = record.getClass();
        while (current != null && !Object.class.equals(current)) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(record);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new PlatformException("Cannot read reference field: " + fieldName, e);
            }
        }
        throw new PlatformException("Cannot find reference field: " + record.getClass().getName() + "." + fieldName);
    }

    private static void writeByFieldName(Object record, String fieldName, Object value) {
        Class<?> current = record.getClass();
        while (current != null && !Object.class.equals(current)) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(record, value);
                return;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new PlatformException("Cannot write reference title field: " + fieldName, e);
            } catch (IllegalArgumentException e) {
                throw new PlatformException("Cannot write reference title field: "
                        + record.getClass().getName() + "." + fieldName
                        + ", value type: " + valueType(value), e);
            }
        }
        throw new PlatformException("Cannot find reference title field: " + record.getClass().getName() + "." + fieldName);
    }

    private static String valueType(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
