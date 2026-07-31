package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record ProjectionGraph(String moduleAlias,
                              String viewCode,
                              List<ProjectionGraphNode> nodes,
                              List<ProjectionGraphEdge> edges,
                              List<ProjectionGraphTransform> transforms) {
    public ProjectionGraph {
        moduleAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        if (viewCode == null || viewCode.isBlank()) {
            throw new IllegalArgumentException("projection graph view code must not be blank");
        }
        viewCode = viewCode.trim();
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        edges = edges == null ? List.of() : List.copyOf(edges);
        transforms = transforms == null ? List.of() : List.copyOf(transforms);
        Set<String> nodeIds = new LinkedHashSet<>();
        for (ProjectionGraphNode node : nodes) {
            if (!nodeIds.add(node.nodeId())) {
                throw new IllegalArgumentException("duplicate projection graph node: " + node.nodeId());
            }
        }
        for (ProjectionGraphEdge edge : edges) {
            if (!nodeIds.contains(edge.sourceNodeId())) {
                throw new IllegalArgumentException("projection graph edge source node is not declared: "
                        + edge.sourceNodeId());
            }
            if (!nodeIds.contains(edge.targetNodeId())) {
                throw new IllegalArgumentException("projection graph edge target node is not declared: "
                        + edge.targetNodeId());
            }
        }
    }

    public List<ProjectionGraphNode> responseFieldNodes() {
        return nodes.stream()
                .filter(ProjectionGraphNode::responseField)
                .toList();
    }

    public List<ProjectionGraphNode> internalReadFieldNodes() {
        return nodes.stream()
                .filter(ProjectionGraphNode::internalReadField)
                .toList();
    }

    public List<RecordReadPostTransform> parsedTransforms() {
        return transforms.stream()
                .map(ProjectionGraphTransform::transform)
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
