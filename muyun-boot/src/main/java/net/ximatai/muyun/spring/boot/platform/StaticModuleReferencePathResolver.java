package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.ability.reference.ModuleFieldRef;
import net.ximatai.muyun.spring.ability.reference.ModuleReferencePath;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class StaticModuleReferencePathResolver {
    private StaticModuleReferencePathResolver() {
    }

    static Traversal resolve(Map<String, StaticModuleDefinition> modules,
                             StaticModuleDefinition start,
                             String relationPath) {
        return resolve(modules, start, relationPath, RelationProjectionPlanningOptions.defaults());
    }

    static Traversal resolve(Map<String, StaticModuleDefinition> modules,
                             StaticModuleDefinition start,
                             String relationPath,
                             RelationProjectionPlanningOptions options) {
        if (relationPath == null || relationPath.isBlank()) {
            return null;
        }
        RelationProjectionPlanningOptions planningOptions = options == null
                ? RelationProjectionPlanningOptions.defaults()
                : options;
        String[] segments = relationPath.split("\\.");
        validateDepth(segments.length, planningOptions.maxJoinDepth(), start, relationPath);
        StaticModuleDefinition current = start;
        String currentAlias = RelationProjectionSqlNames.MAIN_ALIAS;
        List<JoinStep> joins = new ArrayList<>();
        String pathAlias = "";
        for (String segment : segments) {
            if (segment == null || segment.isBlank()) {
                return null;
            }
            ResolvedStep step = resolveDirectReference(modules, current, currentAlias, segment, pathAlias);
            if (step == null) {
                step = resolveInverseReference(modules, current, currentAlias, segment, pathAlias);
            }
            if (step == null) {
                return null;
            }
            joins.add(step.join());
            current = step.definition();
            currentAlias = step.join().tableAlias();
            pathAlias = pathAlias.isBlank() ? segment : pathAlias + "_" + segment;
        }
        return new Traversal(current.entities().getFirst(), currentAlias, List.copyOf(joins));
    }

    static Traversal resolve(Map<String, StaticModuleDefinition> modules,
                             StaticModuleDefinition start,
                             ModuleReferencePath referencePath) {
        return resolve(modules, start, referencePath, RelationProjectionPlanningOptions.defaults());
    }

    static Traversal resolve(Map<String, StaticModuleDefinition> modules,
                             StaticModuleDefinition start,
                             ModuleReferencePath referencePath,
                             RelationProjectionPlanningOptions options) {
        if (referencePath == null) {
            return null;
        }
        RelationProjectionPlanningOptions planningOptions = options == null
                ? RelationProjectionPlanningOptions.defaults()
                : options;
        validateDepth(referencePath.steps().size(), planningOptions.maxJoinDepth(), start, referencePath.toString());
        StaticModuleDefinition current = start;
        String currentAlias = RelationProjectionSqlNames.MAIN_ALIAS;
        List<JoinStep> joins = new ArrayList<>();
        String pathAlias = "";
        for (ModuleReferencePath.Step segment : referencePath.steps()) {
            ResolvedStep step = switch (segment.direction()) {
                case DIRECT -> resolveDirectReference(modules, current, currentAlias,
                        segment.referenceField(), pathAlias);
                case INVERSE -> resolveInverseReference(modules, current, currentAlias,
                        segment, pathAlias);
            };
            if (step == null) {
                return null;
            }
            joins.add(step.join());
            current = step.definition();
            currentAlias = step.join().tableAlias();
            String segmentAlias = snakeCase(segment.referenceField().fieldName());
            pathAlias = pathAlias.isBlank()
                    ? segmentAlias
                    : pathAlias + "_" + segmentAlias;
        }
        if (!matchesModelClass(current, referencePath.targetField().ownerType())) {
            return null;
        }
        return new Traversal(current.entities().getFirst(), currentAlias, List.copyOf(joins));
    }

    private static void validateDepth(int depth,
                                      int maxDepth,
                                      StaticModuleDefinition start,
                                      String path) {
        if (depth > maxDepth) {
            throw new IllegalArgumentException("relation projection reference path depth exceeds limit: "
                    + start.moduleAlias() + "." + path + "." + depth + " > " + maxDepth);
        }
    }

    private static ResolvedStep resolveDirectReference(Map<String, StaticModuleDefinition> modules,
                                                       StaticModuleDefinition current,
                                                       String currentAlias,
                                                       String segment,
                                                       String pathAlias) {
        for (StaticModuleReferenceDefinition reference : current.references()) {
            if (!reference.code().equals(segment)) {
                continue;
            }
            StaticModuleDefinition target = modules.get(reference.targetModuleAlias());
            if (target == null || target.entities().isEmpty()) {
                return null;
            }
            EntityDefinition sourceEntity = current.entities().getFirst();
            EntityDefinition targetEntity = target.entities().getFirst();
            String tableAlias = joinAlias(pathAlias, segment);
            return new ResolvedStep(target, new JoinStep(
                    targetEntity,
                    tableAlias,
                    RelationProjectionCardinality.MANY_TO_ONE,
                    List.of(
                            new RelationProjectionJoinCondition(currentAlias, StandardEntitySchema.TENANT_ID_COLUMN,
                                    tableAlias, StandardEntitySchema.TENANT_ID_COLUMN),
                            new RelationProjectionJoinCondition(currentAlias,
                                    RelationProjectionQueryPlanner.columnName(sourceEntity, reference.sourceField()),
                                    tableAlias,
                                    RelationProjectionQueryPlanner.columnName(targetEntity, reference.targetField()))
                    )
            ));
        }
        return null;
    }

    private static ResolvedStep resolveDirectReference(Map<String, StaticModuleDefinition> modules,
                                                       StaticModuleDefinition current,
                                                       String currentAlias,
                                                       ModuleFieldRef referenceField,
                                                       String pathAlias) {
        if (!matchesModelClass(current, referenceField.ownerType())) {
            return null;
        }
        for (StaticModuleReferenceDefinition reference : current.references()) {
            if (!reference.sourceField().equals(referenceField.fieldName())) {
                continue;
            }
            StaticModuleDefinition target = modules.get(reference.targetModuleAlias());
            if (target == null || target.entities().isEmpty()) {
                return null;
            }
            EntityDefinition sourceEntity = current.entities().getFirst();
            EntityDefinition targetEntity = target.entities().getFirst();
            String tableAlias = joinAlias(pathAlias, referenceField.fieldName());
            return new ResolvedStep(target, new JoinStep(
                    targetEntity,
                    tableAlias,
                    RelationProjectionCardinality.MANY_TO_ONE,
                    List.of(
                            new RelationProjectionJoinCondition(currentAlias, StandardEntitySchema.TENANT_ID_COLUMN,
                                    tableAlias, StandardEntitySchema.TENANT_ID_COLUMN),
                            new RelationProjectionJoinCondition(currentAlias,
                                    RelationProjectionQueryPlanner.columnName(sourceEntity, reference.sourceField()),
                                    tableAlias,
                                    RelationProjectionQueryPlanner.columnName(targetEntity, reference.targetField()))
                    )
            ));
        }
        return null;
    }

    private static ResolvedStep resolveInverseReference(Map<String, StaticModuleDefinition> modules,
                                                        StaticModuleDefinition current,
                                                        String currentAlias,
                                                        String segment,
                                                        String pathAlias) {
        for (StaticModuleDefinition candidate : modules.values()) {
            if (candidate.entities().isEmpty()) {
                continue;
            }
            EntityDefinition candidateEntity = candidate.entities().getFirst();
            if (!segment.equals(candidateEntity.alias())) {
                continue;
            }
            for (StaticModuleReferenceDefinition reference : candidate.references()) {
                if (!reference.targetModuleAlias().equals(current.moduleAlias())) {
                    continue;
                }
                EntityDefinition currentEntity = current.entities().getFirst();
                String tableAlias = joinAlias(pathAlias, segment);
                return new ResolvedStep(candidate, new JoinStep(
                        candidateEntity,
                        tableAlias,
                        RelationProjectionCardinality.ONE_TO_MANY,
                        List.of(
                                new RelationProjectionJoinCondition(currentAlias, StandardEntitySchema.TENANT_ID_COLUMN,
                                        tableAlias, StandardEntitySchema.TENANT_ID_COLUMN),
                                new RelationProjectionJoinCondition(currentAlias,
                                        RelationProjectionQueryPlanner.columnName(currentEntity, reference.targetField()),
                                        tableAlias,
                                        RelationProjectionQueryPlanner.columnName(candidateEntity, reference.sourceField()))
                        )
                ));
            }
        }
        return null;
    }

    private static ResolvedStep resolveInverseReference(Map<String, StaticModuleDefinition> modules,
                                                        StaticModuleDefinition current,
                                                        String currentAlias,
                                                        ModuleReferencePath.Step segment,
                                                        String pathAlias) {
        ModuleFieldRef referenceField = segment.referenceField();
        for (StaticModuleDefinition candidate : modules.values()) {
            if (candidate.entities().isEmpty() || !matchesModelClass(candidate, referenceField.ownerType())) {
                continue;
            }
            EntityDefinition candidateEntity = candidate.entities().getFirst();
            for (StaticModuleReferenceDefinition reference : candidate.references()) {
                if (!reference.sourceField().equals(referenceField.fieldName())
                        || !reference.targetModuleAlias().equals(current.moduleAlias())) {
                    continue;
                }
                EntityDefinition currentEntity = current.entities().getFirst();
                String tableAlias = joinAlias(pathAlias, referenceField.fieldName());
                return new ResolvedStep(candidate, new JoinStep(
                        candidateEntity,
                        tableAlias,
                        segment.safeForPageJoin()
                                ? RelationProjectionCardinality.ONE_TO_ONE
                                : RelationProjectionCardinality.ONE_TO_MANY,
                        List.of(
                                new RelationProjectionJoinCondition(currentAlias, StandardEntitySchema.TENANT_ID_COLUMN,
                                        tableAlias, StandardEntitySchema.TENANT_ID_COLUMN),
                                new RelationProjectionJoinCondition(currentAlias,
                                        RelationProjectionQueryPlanner.columnName(currentEntity, reference.targetField()),
                                        tableAlias,
                                        RelationProjectionQueryPlanner.columnName(candidateEntity, reference.sourceField()))
                        )
                ));
            }
        }
        return null;
    }

    private static boolean matchesModelClass(StaticModuleDefinition definition, Class<?> modelClass) {
        return definition != null
                && definition.modelClass() != null
                && modelClass != null
                && (definition.modelClass().equals(modelClass)
                || modelClass.isAssignableFrom(definition.modelClass())
                || definition.modelClass().isAssignableFrom(modelClass));
    }

    private static String joinAlias(String pathAlias, String segment) {
        String normalizedSegment = snakeCase(segment);
        String value = pathAlias == null || pathAlias.isBlank() ? normalizedSegment : pathAlias + "_" + normalizedSegment;
        return RelationProjectionSqlNames.requireAlias(value, "referenceJoinAlias");
    }

    private static String snakeCase(String value) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (Character.isUpperCase(ch)) {
                if (!result.isEmpty()) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    record JoinStep(EntityDefinition entity,
                    String tableAlias,
                    RelationProjectionCardinality cardinality,
                    List<RelationProjectionJoinCondition> conditions) {
    }

    private record ResolvedStep(StaticModuleDefinition definition,
                                JoinStep join) {
    }

    record Traversal(EntityDefinition entity,
                     String tableAlias,
                     List<JoinStep> joins) {
    }
}
