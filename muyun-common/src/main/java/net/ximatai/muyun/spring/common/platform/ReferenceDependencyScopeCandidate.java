package net.ximatai.muyun.spring.common.platform;

import net.ximatai.muyun.spring.common.util.Preconditions;

/**
 * A metadata-confirmed reference relationship that can be used to derive a source module's data scope.
 *
 * <p>The value is deliberately an opaque authorization value rather than a UI field name. Static and
 * dynamic module resolvers can therefore expose the same catalog without the IAM layer understanding
 * their respective definition formats.</p>
 */
public record ReferenceDependencyScopeCandidate(
        String referenceFieldId,
        String title,
        String targetModuleAlias,
        String targetModuleTitle,
        String referenceActionCode,
        String referenceActionTitle
) {
    public ReferenceDependencyScopeCandidate {
        referenceFieldId = Preconditions.requireText(referenceFieldId, "referenceFieldId");
        title = Preconditions.requireText(title, "title");
        targetModuleAlias = Preconditions.requireText(targetModuleAlias, "targetModuleAlias");
        targetModuleTitle = Preconditions.requireText(targetModuleTitle, "targetModuleTitle");
        referenceActionCode = Preconditions.requireText(referenceActionCode, "referenceActionCode");
        referenceActionTitle = Preconditions.requireText(referenceActionTitle, "referenceActionTitle");
    }
}
