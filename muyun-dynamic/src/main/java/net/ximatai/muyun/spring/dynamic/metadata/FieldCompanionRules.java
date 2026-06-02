package net.ximatai.muyun.spring.dynamic.metadata;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class FieldCompanionRules {
    private FieldCompanionRules() {
    }

    public static String zonedTimestampTimeZoneFieldName(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("zoned timestamp fieldName must not be blank");
        }
        return fieldName + "TimeZone";
    }

    public static String zonedTimestampTimeZoneColumnName(String columnName) {
        if (columnName == null || columnName.isBlank()) {
            throw new IllegalArgumentException("zoned timestamp columnName must not be blank");
        }
        return columnName + "_timezone";
    }

    public static List<FieldCompanionGroup> groups(EntityDefinition entity) {
        return entity.fields().stream()
                .map(FieldCompanionRules::group)
                .flatMap(List::stream)
                .toList();
    }

    public static List<FieldCompanionGroup> group(FieldDefinition owner) {
        if (owner.type() == FieldType.ZONED_TIMESTAMP) {
            return List.of(new FieldCompanionGroup(
                    owner.fieldName(),
                    FieldCompanionKind.ZONED_TIMESTAMP,
                    List.of(new FieldCompanionDefinition(
                            owner.fieldName(),
                            zonedTimestampTimeZoneFieldName(owner.fieldName()),
                            zonedTimestampTimeZoneColumnName(owner.columnName()),
                            FieldType.STRING,
                            FieldCompanionRole.TIME_ZONE,
                            true,
                            true
                    ))
            ));
        }
        return List.of();
    }

    public static Map<String, FieldCompanionDefinition> companionsByField(EntityDefinition entity) {
        return groups(entity).stream()
                .flatMap(group -> group.companions().stream())
                .collect(Collectors.toUnmodifiableMap(FieldCompanionDefinition::fieldName, Function.identity()));
    }

    public static Set<String> companionFieldNames(EntityDefinition entity) {
        return companionsByField(entity).keySet();
    }

    public static Object normalizeCompanionValue(FieldCompanionDefinition companion, Object value) {
        if (companion.role() == FieldCompanionRole.TIME_ZONE) {
            return DynamicFieldValueSupport.normalizeTimeZone(value);
        }
        return DynamicFieldValueSupport.normalize(companion.type(), value);
    }

    public static void validateEntity(EntityDefinition entity) {
        Map<String, FieldDefinition> fieldsByName = entity.fields().stream()
                .collect(Collectors.toMap(FieldDefinition::fieldName, Function.identity()));
        for (FieldCompanionGroup group : groups(entity)) {
            for (FieldCompanionDefinition expected : group.companions()) {
                FieldDefinition actual = fieldsByName.get(expected.fieldName());
                if (actual == null) {
                    throw new ModuleDefinitionException("field companion is missing: "
                            + entity.alias() + "." + expected.fieldName());
                }
                if (actual.type() != expected.type()) {
                    throw new ModuleDefinitionException("field companion type mismatch: "
                            + entity.alias() + "." + expected.fieldName());
                }
                if (!expected.columnName().equals(actual.columnName())) {
                    throw new ModuleDefinitionException("field companion column mismatch: "
                            + entity.alias() + "." + expected.fieldName());
                }
                FieldDefinition owner = fieldsByName.get(group.ownerField());
                if (owner != null && owner.isRequired() && !actual.isRequired()) {
                    throw new ModuleDefinitionException("required field requires required companion: "
                            + entity.alias() + "." + expected.fieldName());
                }
            }
        }
    }

    public static void validateForInsert(EntityDefinition entity, Map<String, Object> values) {
        for (FieldCompanionGroup group : groups(entity)) {
            if (values.get(group.ownerField()) == null) {
                continue;
            }
            for (FieldCompanionDefinition companion : group.companions()) {
                if (companion.requiredWhenOwnerPresent() && values.get(companion.fieldName()) == null) {
                    throw new IllegalArgumentException("field companion is missing: " + companion.fieldName());
                }
            }
        }
    }

    public static void validateForUpdate(EntityDefinition entity, Set<String> explicitFields) {
        for (FieldCompanionGroup group : groups(entity)) {
            if (!explicitFields.contains(group.ownerField())) {
                continue;
            }
            for (FieldCompanionDefinition companion : group.companions()) {
                if (companion.requiredWhenOwnerUpdated() && !explicitFields.contains(companion.fieldName())) {
                    throw new IllegalArgumentException("field companion is missing: " + companion.fieldName());
                }
            }
        }
    }
}
