package net.ximatai.muyun.spring.common.platform;

import net.ximatai.muyun.spring.common.util.Preconditions;

public record ReferenceDependencyScopeRequest(
        String moduleAlias,
        String referenceFieldId,
        String referenceActionCode
) {
    public ReferenceDependencyScopeRequest {
        moduleAlias = Preconditions.requireText(moduleAlias, "moduleAlias");
        referenceFieldId = Preconditions.requireText(referenceFieldId, "referenceFieldId");
        referenceActionCode = Preconditions.requireText(referenceActionCode, "referenceActionCode");
    }
}
