package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.ability.reference.ModuleReadProjection;
import net.ximatai.muyun.spring.ability.security.FieldOutputRenderer;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.common.security.FieldProtectionDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class FieldProtectionProjectionPostProcessor {
    private FieldProtectionProjectionPostProcessor() {
    }

    static boolean hasStorageProtectedOutput(List<StaticModuleDefinition> definitions,
                                             StaticModuleDefinition definition,
                                             RecordReadProjection projection) {
        return outputProtections(definitions, definition, projection).values().stream()
                .anyMatch(FieldProtectionDefinition::hasStorageProtection);
    }

    static List<Map<String, Object>> applySqlOutput(List<StaticModuleDefinition> definitions,
                                                    StaticModuleDefinition definition,
                                                    RecordReadProjection projection,
                                                    List<Map<String, Object>> records,
                                                    FieldOutputContext context) {
        if (records == null || records.isEmpty()) {
            return records;
        }
        Map<String, FieldProtectionDefinition> protections = outputProtections(definitions, definition, projection);
        if (protections.isEmpty()) {
            return records;
        }
        FieldOutputContext outputContext = context == null ? FieldOutputContext.LIST : context;
        return records.stream()
                .map(record -> apply(record, protections, outputContext))
                .toList();
    }

    private static Map<String, Object> apply(Map<String, Object> record,
                                             Map<String, FieldProtectionDefinition> protections,
                                             FieldOutputContext context) {
        Map<String, Object> output = new LinkedHashMap<>(record);
        for (Map.Entry<String, FieldProtectionDefinition> entry : protections.entrySet()) {
            FieldProtectionDefinition protection = entry.getValue();
            String outputField = entry.getKey();
            if (!protection.hasOutputProtection() || protection.hasStorageProtection()
                    || !output.containsKey(outputField)) {
                continue;
            }
            output.put(outputField, FieldOutputRenderer.renderValue(
                    outputField,
                    output.get(outputField),
                    protection,
                    context,
                    null
            ));
        }
        return output;
    }

    private static Map<String, FieldProtectionDefinition> outputProtections(
            List<StaticModuleDefinition> definitions,
            StaticModuleDefinition definition,
            RecordReadProjection projection) {
        if (definition == null || projection == null || definition.entities().isEmpty()) {
            return Map.of();
        }
        Map<String, StaticModuleDefinition> modules = modulesByAlias(definitions, definition);
        Map<String, FieldProtectionDefinition> protections = new LinkedHashMap<>();
        for (ViewFieldRef field : projection.outputFields()) {
            FieldDefinition resolved = resolveOutputField(modules, definition, field);
            if (resolved != null && resolved.protection().enabled()) {
                protections.put(field.fieldName(), resolved.protection());
            }
        }
        return protections;
    }

    private static FieldDefinition resolveOutputField(Map<String, StaticModuleDefinition> modules,
                                                      StaticModuleDefinition definition,
                                                      ViewFieldRef field) {
        if (field.relationCode() != null) {
            return relationField(modules, definition, field.relationCode(), field.fieldName());
        }
        StaticModuleReadProjectionDefinition readProjection = readProjection(definition, field.fieldName());
        if (readProjection != null) {
            return readProjectionField(modules, definition, readProjection);
        }
        return field(definition.entities().getFirst(), field.fieldName());
    }

    private static FieldDefinition relationField(Map<String, StaticModuleDefinition> modules,
                                                 StaticModuleDefinition definition,
                                                 String relationCode,
                                                 String fieldName) {
        FieldDefinition localRelationField = definition.entities().stream()
                .filter(entity -> relationCode.equals(entity.alias()))
                .findFirst()
                .map(entity -> field(entity, fieldName))
                .orElse(null);
        if (localRelationField != null) {
            return localRelationField;
        }
        StaticModuleReferencePathResolver.Traversal traversal =
                StaticModuleReferencePathResolver.resolve(modules, definition, relationCode);
        return traversal == null ? null : field(traversal.entity(), fieldName);
    }

    private static FieldDefinition readProjectionField(Map<String, StaticModuleDefinition> modules,
                                                       StaticModuleDefinition definition,
                                                       StaticModuleReadProjectionDefinition projection) {
        if (projection.projectionType() != ModuleReadProjection.ProjectionType.FIELD) {
            return null;
        }
        StaticModuleReferencePathResolver.Traversal traversal;
        String targetFieldName;
        if (projection.referencePath() != null) {
            traversal = StaticModuleReferencePathResolver.resolve(modules, definition, projection.referencePath());
            targetFieldName = projection.referencePath().targetField().fieldName();
        } else {
            String path = projection.path();
            int lastSeparator = path == null ? -1 : path.lastIndexOf('.');
            if (lastSeparator < 0) {
                return null;
            }
            traversal = StaticModuleReferencePathResolver.resolve(modules, definition, path.substring(0, lastSeparator));
            targetFieldName = path.substring(lastSeparator + 1);
        }
        if (traversal == null) {
            return null;
        }
        return field(traversal.entity(), targetFieldName);
    }

    private static FieldDefinition field(EntityDefinition entity, String fieldName) {
        return entity.fields().stream()
                .filter(field -> field.fieldName().equals(fieldName))
                .findFirst()
                .orElse(null);
    }

    private static StaticModuleReadProjectionDefinition readProjection(StaticModuleDefinition definition,
                                                                       String outputField) {
        return definition.readProjections().stream()
                .filter(item -> item.outputField().equals(outputField))
                .findFirst()
                .orElse(null);
    }

    private static Map<String, StaticModuleDefinition> modulesByAlias(List<StaticModuleDefinition> definitions,
                                                                      StaticModuleDefinition definition) {
        Map<String, StaticModuleDefinition> modules = new LinkedHashMap<>();
        if (definitions != null) {
            definitions.stream()
                    .filter(item -> item != null)
                    .forEach(item -> modules.put(item.moduleAlias(), item));
        }
        modules.putIfAbsent(definition.moduleAlias(), definition);
        return modules;
    }
}
