package net.ximatai.muyun.spring.boot.platform;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecordReadProjectionGraphPlanner {
    private static final String ROOT_NODE_ID = "root";

    private RecordReadProjectionGraphPlanner() {
    }

    public static ProjectionGraph plan(List<StaticModuleDefinition> definitions,
                                       StaticModuleDefinition definition,
                                       RecordReadProjection projection) {
        return plan(definitions, definition, projection, RelationProjectionPlanningOptions.defaults());
    }

    public static ProjectionGraph plan(List<StaticModuleDefinition> definitions,
                                       StaticModuleDefinition definition,
                                       RecordReadProjection projection,
                                       RelationProjectionPlanningOptions options) {
        if (definition == null) {
            throw new IllegalArgumentException("static module definition must not be null");
        }
        if (projection == null) {
            throw new IllegalArgumentException("record read projection must not be null");
        }
        if (!definition.moduleAlias().equals(projection.moduleAlias())) {
            throw new IllegalArgumentException("projection module alias mismatch: "
                    + definition.moduleAlias() + " != " + projection.moduleAlias());
        }
        ProjectionGraph base = RecordReadProjectionGraphAdapter.adapt(projection);
        LinkedHashMap<String, ProjectionGraphNode> nodes = new LinkedHashMap<>();
        for (ProjectionGraphNode node : base.nodes()) {
            nodes.put(node.nodeId(), node);
        }
        List<ProjectionGraphEdge> edges = new ArrayList<>(base.edges());
        Map<String, StaticModuleDefinition> modules =
                RecordReadProjectionReferenceResolver.modulesByAlias(definitions, definition);

        for (ViewFieldRef field : projection.outputFields()) {
            RecordReadProjectionReferenceResolver.ResolvedOutput output =
                    RecordReadProjectionReferenceResolver.resolve(modules, definition, field, options);
            if (output == null) {
                continue;
            }
            appendReferenceEdges(nodes, edges, projection.moduleAlias(), field, output);
        }
        return new ProjectionGraph(
                base.moduleAlias(),
                base.viewCode(),
                List.copyOf(nodes.values()),
                edges,
                base.transforms()
        );
    }

    private static void appendReferenceEdges(LinkedHashMap<String, ProjectionGraphNode> nodes,
                                             List<ProjectionGraphEdge> edges,
                                             String moduleAlias,
                                             ViewFieldRef field,
                                             RecordReadProjectionReferenceResolver.ResolvedOutput output) {
        String previousNodeId = ROOT_NODE_ID;
        for (StaticModuleReferencePathResolver.JoinStep join : output.traversal().joins()) {
            String joinNodeId = joinNodeId(join.tableAlias());
            nodes.putIfAbsent(joinNodeId, ProjectionGraphNode.join(moduleAlias, join.tableAlias()));
            if (!hasReferenceJoinEdge(edges, previousNodeId, joinNodeId)) {
                edges.add(new ProjectionGraphEdge(
                        previousNodeId,
                        joinNodeId,
                        ProjectionGraphEdgeKind.REFERENCE_JOIN,
                        join.tableAlias(),
                        join.tableAlias(),
                        join.cardinality(),
                        join.conditions()
                ));
            }
            previousNodeId = joinNodeId;
        }
        String outputNodeId = field.relationCode() == null
                ? mainNodeId(field.fieldName())
                : relationNodeId(field.relationCode(), field.fieldName());
        edges.add(new ProjectionGraphEdge(
                previousNodeId,
                outputNodeId,
                ProjectionGraphEdgeKind.REFERENCE_OUTPUT_FIELD,
                output.existsProjection() ? "exists:" + output.targetFieldName() : output.targetFieldName()
        ));
    }

    private static boolean hasReferenceJoinEdge(List<ProjectionGraphEdge> edges,
                                                String sourceNodeId,
                                                String targetNodeId) {
        return edges.stream()
                .anyMatch(edge -> edge.edgeKind() == ProjectionGraphEdgeKind.REFERENCE_JOIN
                        && edge.sourceNodeId().equals(sourceNodeId)
                        && edge.targetNodeId().equals(targetNodeId));
    }

    private static String mainNodeId(String fieldName) {
        return "main:" + fieldName;
    }

    private static String relationNodeId(String relationCode, String fieldName) {
        return "relation:" + relationCode + ":" + fieldName;
    }

    private static String joinNodeId(String tableAlias) {
        return "join:" + tableAlias;
    }

}
