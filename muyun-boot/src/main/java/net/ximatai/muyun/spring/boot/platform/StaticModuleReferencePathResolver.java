package net.ximatai.muyun.spring.boot.platform;

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
        if (relationPath == null || relationPath.isBlank()) {
            return null;
        }
        String[] segments = relationPath.split("\\.");
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

    private static String joinAlias(String pathAlias, String segment) {
        String value = pathAlias == null || pathAlias.isBlank() ? segment : pathAlias + "_" + segment;
        return RelationProjectionSqlNames.requireAlias(value, "referenceJoinAlias");
    }

    record JoinStep(EntityDefinition entity,
                    String tableAlias,
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
