package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

import java.util.List;

public record RecordReadProjection(String moduleAlias,
                                   String viewCode,
                                   String actionCode,
                                   String permissionCode,
                                   String permissionActionCode,
                                   List<String> fieldReadPolicies,
                                   List<ViewFieldRef> outputFields,
                                   List<String> internalReadFields,
                                   List<String> postReadTransforms) {
    public RecordReadProjection {
        moduleAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        if (viewCode == null || viewCode.isBlank()) {
            throw new IllegalArgumentException("record read projection view code must not be blank");
        }
        viewCode = viewCode.trim();
        actionCode = actionCode == null || actionCode.isBlank() ? null : actionCode.trim();
        permissionCode = permissionCode == null || permissionCode.isBlank() ? null : permissionCode.trim();
        permissionActionCode = permissionActionCode == null || permissionActionCode.isBlank()
                ? null
                : permissionActionCode.trim();
        fieldReadPolicies = fieldReadPolicies == null ? List.of() : List.copyOf(fieldReadPolicies);
        outputFields = outputFields == null ? List.of() : List.copyOf(outputFields);
        internalReadFields = internalReadFields == null ? List.of() : List.copyOf(internalReadFields);
        postReadTransforms = postReadTransforms == null ? List.of() : List.copyOf(postReadTransforms);
    }

    public RecordReadProjection(String moduleAlias,
                                String viewCode,
                                List<ViewFieldRef> outputFields,
                                List<String> internalReadFields,
                                List<String> postReadTransforms) {
        this(moduleAlias, viewCode, null, null, null, List.of(), outputFields, internalReadFields,
                postReadTransforms);
    }

    public List<String> readFields() {
        return java.util.stream.Stream.concat(internalReadFields.stream(),
                        outputFields.stream().map(ViewFieldRef::fieldName))
                .distinct()
                .toList();
    }
}
