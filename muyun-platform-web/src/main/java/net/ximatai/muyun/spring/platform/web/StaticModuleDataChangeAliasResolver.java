package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.web.*;

import net.ximatai.muyun.spring.ability.action.DataChangeModuleAliasResolver;
import net.ximatai.muyun.spring.platform.web.StaticModuleDefinitionCatalog;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;

public class StaticModuleDataChangeAliasResolver implements DataChangeModuleAliasResolver {
    private final StaticModuleDefinitionCatalog staticModuleDefinitionCatalog;

    public StaticModuleDataChangeAliasResolver(StaticModuleDefinitionCatalog staticModuleDefinitionCatalog) {
        this.staticModuleDefinitionCatalog = staticModuleDefinitionCatalog;
    }

    @Override
    public String moduleAlias(Class<?> moduleType) {
        String alias = moduleAliasConstant(moduleType);
        staticModuleDefinitionCatalog.find(alias)
                .orElseThrow(() -> new IllegalArgumentException("static module is not registered: " + alias));
        return alias;
    }

    private String moduleAliasConstant(Class<?> moduleType) {
        try {
            Object value = moduleType.getField("MODULE_ALIAS").get(null);
            if (value instanceof String alias && !alias.isBlank()) {
                return PlatformNameRules.requireModuleAlias(alias.trim());
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through to a clear platform error.
        }
        throw new IllegalArgumentException("static module type must expose MODULE_ALIAS: " + moduleType.getName());
    }
}
