package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

/** Declares a reusable master-list scope for a standard list module workspace. */
public record ScopedListWorkspaceDefinition(String scopeModuleAlias,
                                            String scopeField,
                                            String scopeTitle,
                                            String scopeSearchPlaceholder) {
    public ScopedListWorkspaceDefinition {
        scopeModuleAlias = PlatformNameRules.requireModuleAlias(scopeModuleAlias);
        scopeField = PlatformNameRules.requireFieldName(scopeField, "scopeField");
        scopeTitle = scopeTitle == null || scopeTitle.isBlank() ? "范围" : scopeTitle.trim();
        scopeSearchPlaceholder = scopeSearchPlaceholder == null || scopeSearchPlaceholder.isBlank()
                ? "搜索" + scopeTitle : scopeSearchPlaceholder.trim();
    }
}
