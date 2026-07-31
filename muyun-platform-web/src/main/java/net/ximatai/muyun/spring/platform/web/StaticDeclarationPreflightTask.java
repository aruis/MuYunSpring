package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.platform.runtime.PlatformBootstrapTask;

import java.util.List;

/** Validates the complete static application/module declaration graph before reconciliation writes. */
public class StaticDeclarationPreflightTask implements PlatformBootstrapTask {
    private final StaticApplicationDefinitionCatalog applicationCatalog;
    private final StaticModuleDefinitionCatalog moduleCatalog;

    public StaticDeclarationPreflightTask(StaticApplicationDefinitionCatalog applicationCatalog,
                                          StaticModuleDefinitionCatalog moduleCatalog) {
        this.applicationCatalog = applicationCatalog;
        this.moduleCatalog = moduleCatalog;
    }

    @Override
    public String name() {
        return "platform.static-declaration-preflight";
    }

    @Override
    public int order() {
        return -20;
    }

    @Override
    public void run() {
        validateApplicationOwnership(moduleCatalog.definitions(), applicationCatalog);
    }

    static void validateApplicationOwnership(List<StaticModuleDefinition> definitions,
                                             StaticApplicationDefinitionCatalog applicationCatalog) {
        if (applicationCatalog == null) {
            return;
        }
        for (StaticModuleDefinition definition : definitions) {
            if (applicationCatalog.find(definition.applicationAlias()).isEmpty()) {
                throw new IllegalStateException("static references undeclared static application: "
                        + definition.moduleAlias() + " -> " + definition.applicationAlias());
            }
        }
    }
}
