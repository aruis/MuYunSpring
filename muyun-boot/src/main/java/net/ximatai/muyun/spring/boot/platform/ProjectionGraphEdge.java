package net.ximatai.muyun.spring.boot.platform;

import java.util.List;

public record ProjectionGraphEdge(String sourceNodeId,
                                  String targetNodeId,
                                  ProjectionGraphEdgeKind edgeKind,
                                  String path,
                                  String tableAlias,
                                  RelationProjectionCardinality cardinality,
                                  List<RelationProjectionJoinCondition> joinConditions) {
    public ProjectionGraphEdge(String sourceNodeId,
                               String targetNodeId,
                               ProjectionGraphEdgeKind edgeKind,
                               String path) {
        this(sourceNodeId, targetNodeId, edgeKind, path, null, null, List.of());
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
    }
}
