package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

import java.util.List;

public record RecordReadProjection(String moduleAlias,
                                   String viewCode,
                                   List<ViewFieldRef> outputFields,
                                   List<String> requiredPlatformFields,
                                   List<String> postReadTransforms) {
    public RecordReadProjection {
        moduleAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        if (viewCode == null || viewCode.isBlank()) {
            throw new IllegalArgumentException("record read projection view code must not be blank");
        }
        viewCode = viewCode.trim();
        outputFields = outputFields == null ? List.of() : List.copyOf(outputFields);
        requiredPlatformFields = requiredPlatformFields == null ? List.of() : List.copyOf(requiredPlatformFields);
        postReadTransforms = postReadTransforms == null ? List.of() : List.copyOf(postReadTransforms);
    }

    public List<String> readFields() {
        return java.util.stream.Stream.concat(requiredPlatformFields.stream(),
                        outputFields.stream().map(ViewFieldRef::fieldName))
                .distinct()
                .toList();
    }
}
