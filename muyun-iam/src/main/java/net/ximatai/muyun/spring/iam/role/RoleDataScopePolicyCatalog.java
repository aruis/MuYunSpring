package net.ximatai.muyun.spring.iam.role;

import java.util.List;

/** Stable authorization-workbench catalog; UI clients must not duplicate policy labels or availability rules. */
public record RoleDataScopePolicyCatalog(
        String roleId,
        List<Option> options,
        List<ReferenceDependency> referenceDependencies
) {
    public RoleDataScopePolicyCatalog {
        options = options == null ? List.of() : List.copyOf(options);
        referenceDependencies = referenceDependencies == null ? List.of() : List.copyOf(referenceDependencies);
    }

    public record Option(DataScopePolicy code, String title) {
    }

    public record ReferenceDependency(
            String referenceFieldId,
            String title,
            String targetModuleAlias,
            String targetModuleTitle,
            String referenceActionCode,
            String referenceActionTitle
    ) {
    }
}
