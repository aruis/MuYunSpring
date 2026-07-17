package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

public record ProjectionGraphNode(String nodeId,
                                  ProjectionGraphNodeKind nodeKind,
                                  String moduleAlias,
                                  String relationCode,
                                  String fieldName,
                                  String fieldId,
                                  boolean responseField,
                                  boolean internalReadField) {
    public ProjectionGraphNode {
        if (nodeId == null || nodeId.isBlank()) {
            throw new IllegalArgumentException("projection graph node id must not be blank");
        }
        nodeId = nodeId.trim();
        nodeKind = nodeKind == null ? ProjectionGraphNodeKind.FIELD : nodeKind;
        moduleAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        relationCode = relationCode == null || relationCode.isBlank() ? null : relationCode.trim();
        fieldName = fieldName == null || fieldName.isBlank()
                ? null
                : PlatformNameRules.requireFieldName(fieldName, "projectionGraphFieldName");
        fieldId = fieldId == null || fieldId.isBlank() ? null : fieldId.trim();
        if (nodeKind == ProjectionGraphNodeKind.FIELD && fieldName == null) {
            throw new IllegalArgumentException("projection graph field node requires field name: " + nodeId);
        }
        if (nodeKind == ProjectionGraphNodeKind.ROOT && (relationCode != null || fieldName != null || fieldId != null)) {
            throw new IllegalArgumentException("projection graph root node must not bind field metadata: " + nodeId);
        }
    }

    public static ProjectionGraphNode root(String moduleAlias) {
        return new ProjectionGraphNode("root", ProjectionGraphNodeKind.ROOT, moduleAlias,
                null, null, null, false, false);
    }
}
