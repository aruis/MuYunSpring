package net.ximatai.muyun.spring.boot.platform;

public record ProjectionGraphEdge(String sourceNodeId,
                                  String targetNodeId,
                                  ProjectionGraphEdgeKind edgeKind,
                                  String path) {
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
    }
}
