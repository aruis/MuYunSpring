package net.ximatai.muyun.spring.boot.platform;

import java.util.List;

public record ProjectionGraphEdge(String sourceNodeId,
                                  String targetNodeId,
                                  ProjectionGraphEdgeKind edgeKind,
                                  String path,
                                  String tableAlias,
                                  String outputFieldName,
                                  String targetFieldName,
                                  boolean existsProjection,
                                  RelationProjectionCardinality cardinality,
                                  List<RelationProjectionJoinCondition> joinConditions) {
    public ProjectionGraphEdge(String sourceNodeId,
                               String targetNodeId,
                               ProjectionGraphEdgeKind edgeKind,
                               String path) {
        this(sourceNodeId, targetNodeId, edgeKind, path, null, null, null, false, null, List.of());
    }

    public static ProjectionGraphEdge referenceJoin(String sourceNodeId,
                                                    String targetNodeId,
                                                    String tableAlias,
                                                    RelationProjectionCardinality cardinality,
                                                    List<RelationProjectionJoinCondition> joinConditions) {
        return new ProjectionGraphEdge(sourceNodeId, targetNodeId, ProjectionGraphEdgeKind.REFERENCE_JOIN,
                tableAlias, tableAlias, null, null, false, cardinality, joinConditions);
    }

    public static ProjectionGraphEdge referenceOutput(String sourceNodeId,
                                                      String targetNodeId,
                                                      String outputFieldName,
                                                      String targetFieldName,
                                                      boolean existsProjection) {
        return new ProjectionGraphEdge(sourceNodeId, targetNodeId, ProjectionGraphEdgeKind.REFERENCE_OUTPUT_FIELD,
                targetFieldName, null, outputFieldName, targetFieldName, existsProjection, null, List.of());
    }

    public ProjectionGraphEdge {
        if (sourceNodeId == null || sourceNodeId.isBlank()) {
            throw new IllegalArgumentException("projection graph edge source node id must not be blank");
        }
        if (targetNodeId == null || targetNodeId.isBlank()) {
            throw new IllegalArgumentException("projection graph edge target node id must not be blank");
        }
        sourceNodeId = sourceNodeId.trim();
        targetNodeId = targetNodeId.trim();
        edgeKind = edgeKind == null ? ProjectionGraphEdgeKind.MAIN_OUTPUT_FIELD : edgeKind;
        path = path == null || path.isBlank() ? null : path.trim();
        tableAlias = tableAlias == null || tableAlias.isBlank() ? null : tableAlias.trim();
        outputFieldName = outputFieldName == null || outputFieldName.isBlank()
                ? null
                : net.ximatai.muyun.spring.common.util.PlatformNameRules.requireFieldName(outputFieldName,
                "projectionGraphOutputField");
        targetFieldName = targetFieldName == null || targetFieldName.isBlank()
                ? null
                : net.ximatai.muyun.spring.common.util.PlatformNameRules.requireFieldName(targetFieldName,
                "projectionGraphTargetField");
        joinConditions = joinConditions == null ? List.of() : List.copyOf(joinConditions);
        if (edgeKind == ProjectionGraphEdgeKind.REFERENCE_JOIN) {
            if (tableAlias == null) {
                throw new IllegalArgumentException("projection graph reference join edge requires table alias");
            }
            if (cardinality == null) {
                throw new IllegalArgumentException("projection graph reference join edge requires cardinality");
            }
            if (joinConditions.isEmpty()) {
                throw new IllegalArgumentException("projection graph reference join edge requires join conditions");
            }
        }
        if (edgeKind == ProjectionGraphEdgeKind.REFERENCE_OUTPUT_FIELD) {
            if (outputFieldName == null) {
                throw new IllegalArgumentException("projection graph reference output edge requires output field");
            }
            if (targetFieldName == null) {
                throw new IllegalArgumentException("projection graph reference output edge requires target field");
            }
        } else if (existsProjection) {
            throw new IllegalArgumentException("projection graph exists projection is only valid for reference output edge");
        }
    }
}
