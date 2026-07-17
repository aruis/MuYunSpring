package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.ability.reference.ModuleReadProjection;

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
        Map<String, StaticModuleDefinition> modules = modulesByAlias(definitions);
        if (!modules.containsKey(definition.moduleAlias())) {
            LinkedHashMap<String, StaticModuleDefinition> merged = new LinkedHashMap<>(modules);
            merged.put(definition.moduleAlias(), definition);
            modules = java.util.Collections.unmodifiableMap(merged);
        }

        for (ViewFieldRef field : projection.outputFields()) {
            ResolvedReferenceOutput output = resolveReferenceOutput(modules, definition, field, options);
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
                                             ResolvedReferenceOutput output) {
        String previousNodeId = ROOT_NODE_ID;
        for (StaticModuleReferencePathResolver.JoinStep join : output.traversal().joins()) {
            String joinNodeId = joinNodeId(join.tableAlias());
            nodes.putIfAbsent(joinNodeId, ProjectionGraphNode.join(moduleAlias, join.tableAlias()));
            edges.add(new ProjectionGraphEdge(
                    previousNodeId,
                    joinNodeId,
                    ProjectionGraphEdgeKind.REFERENCE_JOIN,
                    join.tableAlias(),
                    join.tableAlias(),
                    join.cardinality(),
                    join.conditions()
            ));
            previousNodeId = joinNodeId;
        }
        String outputNodeId = field.relationCode() == null
                ? mainNodeId(field.fieldName())
                : relationNodeId(field.relationCode(), field.fieldName());
        edges.add(new ProjectionGraphEdge(
                previousNodeId,
                outputNodeId,
                ProjectionGraphEdgeKind.REFERENCE_OUTPUT_FIELD,
                output.targetFieldName()
        ));
    }

    private static ResolvedReferenceOutput resolveReferenceOutput(Map<String, StaticModuleDefinition> modules,
                                                                  StaticModuleDefinition definition,
                                                                  ViewFieldRef field,
                                                                  RelationProjectionPlanningOptions options) {
        StaticModuleReadProjectionDefinition readProjection = field.relationCode() == null
                ? readProjection(definition, field.fieldName())
                : null;
        StaticModuleReferencePathResolver.Traversal traversal;
        String targetFieldName;
        String unresolvedPath;
        if (readProjection != null && readProjection.referencePath() != null) {
            traversal = StaticModuleReferencePathResolver.resolve(modules, definition,
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
            traversal = StaticModuleReferencePathResolver.resolve(modules, definition, relationPath, options);
            unresolvedPath = relationPath;
        }
        if (traversal == null) {
            if (readProjection != null) {
                throw new IllegalArgumentException("projection reference path is not declared: "
                        + definition.moduleAlias() + "." + readProjection.outputField() + "." + unresolvedPath);
            }
            return null;
        }
        for (StaticModuleReferencePathResolver.JoinStep join : traversal.joins()) {
            if (!join.cardinality().safeForPageJoin()) {
                throw new IllegalArgumentException("projection reference path cardinality is not safe for page join: "
                        + definition.moduleAlias() + "." + field.fieldName() + "."
                        + join.tableAlias() + "." + join.cardinality());
            }
        }
        if (readProjection != null && readProjection.projectionType() == ModuleReadProjection.ProjectionType.EXISTS) {
            targetFieldName = "exists:" + targetFieldName;
        }
        return new ResolvedReferenceOutput(traversal, targetFieldName);
    }

    private static String relationFieldPath(ViewFieldRef field) {
        return field.relationCode() == null ? null : field.relationCode() + "." + field.fieldName();
    }

    private static Map<String, StaticModuleDefinition> modulesByAlias(List<StaticModuleDefinition> definitions) {
        LinkedHashMap<String, StaticModuleDefinition> byAlias = new LinkedHashMap<>();
        if (definitions != null) {
            for (StaticModuleDefinition definition : definitions) {
                if (definition != null) {
                    byAlias.putIfAbsent(definition.moduleAlias(), definition);
                }
            }
        }
        return java.util.Collections.unmodifiableMap(byAlias);
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

    private static String mainNodeId(String fieldName) {
        return "main:" + fieldName;
    }

    private static String relationNodeId(String relationCode, String fieldName) {
        return "relation:" + relationCode + ":" + fieldName;
    }

    private static String joinNodeId(String tableAlias) {
        return "join:" + tableAlias;
    }

    private record ResolvedReferenceOutput(StaticModuleReferencePathResolver.Traversal traversal,
                                           String targetFieldName) {
    }
}
