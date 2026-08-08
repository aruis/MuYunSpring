package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

/**
 * Declares a standard master-list workspace for a module.
 *
 * <p>The selected scope is passed to the consumer module through {@link #queryCriteriaKey()} and
 * is prefilled into {@link #scopeField()} when a new record is created from a selected scope.
 * {@link #createPolicy()} can additionally require that selection. The compiler verifies that
 * the field is a single-valued reference to {@link #scopeModuleAlias()}.</p>
 */
public record ScopedListWorkspaceDefinition(String scopeModuleAlias,
                                            String scopeField,
                                            String queryCriteriaKey,
                                            String scopeTitle,
                                            String scopeSearchPlaceholder,
                                            boolean showScopeItemSubtitle,
                                            ScopedListWorkspaceCreatePolicy createPolicy,
                                            boolean manageScopeTree) {
    public ScopedListWorkspaceDefinition {
        scopeModuleAlias = PlatformNameRules.requireModuleAlias(scopeModuleAlias);
        scopeField = PlatformNameRules.requireFieldName(scopeField, "scopeField");
        queryCriteriaKey = PlatformNameRules.requireFieldName(
                queryCriteriaKey == null || queryCriteriaKey.isBlank() ? scopeField : queryCriteriaKey,
                "queryCriteriaKey");
        scopeTitle = scopeTitle == null || scopeTitle.isBlank() ? "范围" : scopeTitle.trim();
        scopeSearchPlaceholder = scopeSearchPlaceholder == null || scopeSearchPlaceholder.isBlank()
                ? "搜索" + scopeTitle : scopeSearchPlaceholder.trim();
        createPolicy = createPolicy == null ? ScopedListWorkspaceCreatePolicy.ALLOW_UNSCOPED : createPolicy;
    }

    public ScopedListWorkspaceDefinition(String scopeModuleAlias, String scopeField,
                                         String scopeTitle, String scopeSearchPlaceholder) {
        this(scopeModuleAlias, scopeField, scopeField, scopeTitle, scopeSearchPlaceholder, false,
                ScopedListWorkspaceCreatePolicy.ALLOW_UNSCOPED, false);
    }

    public ScopedListWorkspaceDefinition(String scopeModuleAlias, String scopeField, String queryCriteriaKey,
                                         String scopeTitle, String scopeSearchPlaceholder,
                                         boolean showScopeItemSubtitle,
                                         ScopedListWorkspaceCreatePolicy createPolicy) {
        this(scopeModuleAlias, scopeField, queryCriteriaKey, scopeTitle, scopeSearchPlaceholder,
                showScopeItemSubtitle, createPolicy, false);
    }
}
