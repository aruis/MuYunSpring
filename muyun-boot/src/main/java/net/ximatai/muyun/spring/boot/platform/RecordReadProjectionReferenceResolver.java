package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.ability.reference.ModuleReadProjection;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RecordReadProjectionReferenceResolver {
    private RecordReadProjectionReferenceResolver() {
    }

    static Map<String, StaticModuleDefinition> modulesByAlias(List<StaticModuleDefinition> definitions,
                                                              StaticModuleDefinition definition) {
        LinkedHashMap<String, StaticModuleDefinition> modules = new LinkedHashMap<>();
        if (definitions != null) {
            for (StaticModuleDefinition item : definitions) {
                if (item != null) {
                    modules.putIfAbsent(item.moduleAlias(), item);
                }
            }
        }
        if (definition != null) {
            modules.putIfAbsent(definition.moduleAlias(), definition);
        }
        return java.util.Collections.unmodifiableMap(modules);
    }

    static ResolvedOutput resolve(Map<String, StaticModuleDefinition> modules,
                                  StaticModuleDefinition definition,
                                  ViewFieldRef field,
                                  RelationProjectionPlanningOptions options) {
        StaticModuleReadProjectionDefinition readProjection = field.relationCode() == null
                ? readProjection(definition, field.fieldName())
                : null;
        StaticReferencePathResolver.Traversal traversal;
        String targetFieldName;
        String unresolvedPath;
        if (readProjection != null && readProjection.referencePath() != null) {
            traversal = StaticReferencePathResolver.resolve(modules, definition,
                    readProjection.referencePath(), options);
            targetFieldName = readProjection.referencePath().targetField().fieldName();
            unresolvedPath = readProjection.referencePath().toString();
        } else {
            String path = readProjection == null
                    ? relationFieldPath(field)
                    : readProjection.path();
            int lastSeparator = path == null ? -1 : path.lastIndexOf('.');
            if (lastSeparator < 0) {
                if (readProjection != null) {
                    throw new IllegalArgumentException("projection reference path is invalid: "
                            + definition.moduleAlias() + "." + readProjection.outputField() + "." + path);
                }
                return null;
            }
            String relationPath = path.substring(0, lastSeparator);
            targetFieldName = path.substring(lastSeparator + 1);
            traversal = StaticReferencePathResolver.resolve(modules, definition, relationPath, options);
            unresolvedPath = relationPath;
        }
        if (traversal == null) {
            if (readProjection != null) {
                throw new IllegalArgumentException("projection reference path is not declared: "
                        + definition.moduleAlias() + "." + readProjection.outputField() + "." + unresolvedPath);
            }
            return null;
        }
        for (StaticReferencePathResolver.JoinStep join : traversal.joins()) {
            if (!join.cardinality().safeForPageJoin()) {
                throw new IllegalArgumentException("projection reference path cardinality is not safe for page join: "
                        + definition.moduleAlias() + "." + field.fieldName() + "."
                        + join.tableAlias() + "." + join.cardinality());
            }
        }
        boolean existsProjection = readProjection != null
                && readProjection.projectionType() == ModuleReadProjection.ProjectionType.EXISTS;
        return new ResolvedOutput(traversal, targetFieldName, existsProjection, readProjection);
    }

    private static String relationFieldPath(ViewFieldRef field) {
        return field.relationCode() == null ? null : field.relationCode() + "." + field.fieldName();
    }

    private static StaticModuleReadProjectionDefinition readProjection(StaticModuleDefinition definition,
                                                                       String outputField) {
        if (definition == null || outputField == null || outputField.isBlank()) {
            return null;
        }
        return definition.readProjections().stream()
                .filter(projection -> projection.outputField().equals(outputField))
                .findFirst()
                .orElse(null);
    }

    record ResolvedOutput(StaticReferencePathResolver.Traversal traversal,
                          String targetFieldName,
                          boolean existsProjection,
                          StaticModuleReadProjectionDefinition readProjection) {
    }
}
