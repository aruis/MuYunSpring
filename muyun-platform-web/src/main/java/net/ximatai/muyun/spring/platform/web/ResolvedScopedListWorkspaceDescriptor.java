package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

/** Source-neutral web descriptor for a standard master-list workspace. */
public record ResolvedScopedListWorkspaceDescriptor(String scopeModuleAlias,
                                                    String scopeField,
                                                    String queryCriteriaKey,
                                                    String scopeTitle,
                                                    String scopeSearchPlaceholder,
                                                    boolean showScopeItemSubtitle,
                                                    ScopedListWorkspaceCreatePolicy createPolicy) {
    public ResolvedScopedListWorkspaceDescriptor {
        scopeModuleAlias = PlatformNameRules.requireModuleAlias(scopeModuleAlias);
        scopeField = PlatformNameRules.requireFieldName(scopeField, "scopeField");
        queryCriteriaKey = PlatformNameRules.requireFieldName(queryCriteriaKey, "queryCriteriaKey");
        scopeTitle = scopeTitle == null || scopeTitle.isBlank() ? "范围" : scopeTitle.trim();
        scopeSearchPlaceholder = scopeSearchPlaceholder == null || scopeSearchPlaceholder.isBlank()
                ? "搜索" + scopeTitle : scopeSearchPlaceholder.trim();
        createPolicy = createPolicy == null ? ScopedListWorkspaceCreatePolicy.ALLOW_UNSCOPED : createPolicy;
    }

    static ResolvedScopedListWorkspaceDescriptor from(ScopedListWorkspaceDefinition definition) {
        if (definition == null) return null;
        return new ResolvedScopedListWorkspaceDescriptor(definition.scopeModuleAlias(), definition.scopeField(),
                definition.queryCriteriaKey(), definition.scopeTitle(), definition.scopeSearchPlaceholder(),
                definition.showScopeItemSubtitle(), definition.createPolicy());
    }
}
