package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.ability.PlatformManagedMutationContext;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleAction;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StaticModuleDefinitionRegistrar implements PlatformBootstrapTask {
    private final PlatformModuleService moduleService;
    private final PlatformModuleActionService actionService;
    private final StaticModuleDefinitionCatalog definitionCatalog;
    private final boolean disablesStaleSystemManagedModules;
    private final StaticApplicationDefinitionCatalog applicationCatalog;

    public StaticModuleDefinitionRegistrar(PlatformModuleService moduleService,
                                           PlatformModuleActionService actionService,
                                           List<StaticModuleDefinition> definitions) {
        this(moduleService, actionService, definitions, List.of());
    }

    public StaticModuleDefinitionRegistrar(PlatformModuleService moduleService,
                                           PlatformModuleActionService actionService,
                                           List<StaticModuleDefinition> definitions,
                                           List<StaticModuleDefinitionScanner> scanners) {
        this(moduleService, actionService,
                new StaticModuleDefinitionCatalog(definitions, scanners),
                scanners != null && !scanners.isEmpty());
    }

    public StaticModuleDefinitionRegistrar(PlatformModuleService moduleService,
                                           PlatformModuleActionService actionService,
                                           StaticModuleDefinitionCatalog definitionCatalog,
                                           boolean disablesStaleSystemManagedModules) {
        this(moduleService, actionService, definitionCatalog, disablesStaleSystemManagedModules, null);
    }

    public StaticModuleDefinitionRegistrar(PlatformModuleService moduleService,
                                           PlatformModuleActionService actionService,
                                           StaticModuleDefinitionCatalog definitionCatalog,
                                           boolean disablesStaleSystemManagedModules,
                                           StaticApplicationDefinitionCatalog applicationCatalog) {
        this.moduleService = moduleService;
        this.actionService = actionService;
        this.definitionCatalog = definitionCatalog == null
                ? new StaticModuleDefinitionCatalog(List.of())
                : definitionCatalog;
        this.disablesStaleSystemManagedModules = disablesStaleSystemManagedModules;
        this.applicationCatalog = applicationCatalog;
    }

    @Override
    public void run() {
        registerAll();
    }

    @Override
    public int order() {
        return 0;
    }

    public void registerAll() {
        try (TenantContext.Scope ignored = TenantContext.system("register static modules")) {
            List<StaticModuleDefinition> allDefinitions = definitionCatalog.definitions();
            validateApplications(allDefinitions);
            PlatformManagedMutationContext.runAsPlatformManaged(() -> {
                for (StaticModuleDefinition definition : allDefinitions) {
                    registerModule(definition);
                    registerActions(definition);
                }
                disableStaleSystemManagedModules(allDefinitions);
            });
        }
    }

    private void validateApplications(List<StaticModuleDefinition> definitions) {
        if (applicationCatalog == null) {
            return;
        }
        for (StaticModuleDefinition definition : definitions) {
            if (applicationCatalog.find(definition.applicationAlias()).isEmpty()) {
                throw new IllegalStateException("static module references undeclared static application: "
                        + definition.moduleAlias() + " -> " + definition.applicationAlias());
            }
        }
    }

    private void registerModule(StaticModuleDefinition definition) {
        PlatformModule module = moduleService.select(definition.moduleAlias());
        if (module == null) {
            module = new PlatformModule();
            module.setAlias(definition.moduleAlias());
            module.setApplicationAlias(definition.applicationAlias());
            module.setParentId(definition.parentModuleAlias() == null
                    ? TreeAbility.ROOT_ID
                    : definition.parentModuleAlias());
            module.setTitle(definition.title());
            module.setModuleKind(ModuleKind.STATIC);
            module.setEntryType(definition.entryType());
            module.setEntryRoute(definition.entryRoute());
            module.setEntryExternalUrl(definition.entryExternalUrl());
            module.setSystemManaged(Boolean.TRUE);
            moduleService.insert(module);
            return;
        }
        module.setApplicationAlias(definition.applicationAlias());
        module.setParentId(definition.parentModuleAlias() == null
                ? TreeAbility.ROOT_ID
                : definition.parentModuleAlias());
        module.setTitle(definition.title());
        module.setModuleKind(ModuleKind.STATIC);
        module.setEntryType(definition.entryType());
        module.setEntryRoute(definition.entryRoute());
        module.setEntryExternalUrl(definition.entryExternalUrl());
        module.setSystemManaged(Boolean.TRUE);
        moduleService.update(module);
    }

    private void registerActions(StaticModuleDefinition definition) {
        int order = 1;
        for (StaticModuleActionDefinition actionDefinition : definition.actions()) {
            PlatformModuleAction action = actionService.findByModuleAliasAndActionCode(
                    definition.moduleAlias(), actionDefinition.actionCode());
            if (action == null) {
                action = new PlatformModuleAction();
                action.setModuleAlias(definition.moduleAlias());
                action.setActionCode(actionDefinition.actionCode());
            }
            action.setPermissionActionCode(actionDefinition.permissionActionCode());
            action.setTitle(actionDefinition.title());
            action.setCategory(actionDefinition.category());
            action.setActionLevel(actionDefinition.actionLevel());
            action.setAccessMode(actionDefinition.accessMode());
            action.setActionAuth(actionDefinition.actionAuth());
            action.setDataAuth(actionDefinition.dataAuth());
            action.setDefaultGrantPolicy(actionDefinition.defaultGrantPolicy());
            action.setExecutorType(actionDefinition.executorType());
            action.setExecutorKey(actionDefinition.executorKey());
            action.setSystemManaged(Boolean.TRUE);
            action.setEnabled(Boolean.TRUE);
            action.setSortOrder(order++);
            if (action.getId() == null || action.getId().isBlank()) {
                actionService.insert(action);
            } else {
                actionService.update(action);
            }
        }
    }

    private void disableStaleSystemManagedModules(List<StaticModuleDefinition> definitions) {
        if (!disablesStaleSystemManagedModules) {
            return;
        }
        Set<String> currentModuleAliases = new HashSet<>();
        for (StaticModuleDefinition definition : definitions) {
            currentModuleAliases.add(definition.moduleAlias());
        }
        for (PlatformModule module : moduleService.listSystemManagedStaticModules()) {
            if (currentModuleAliases.contains(module.getAlias())) {
                continue;
            }
            for (PlatformModuleAction action : actionService.listSystemManagedActions(module.getAlias())) {
                actionService.disable(action.getId());
            }
            moduleService.disable(module.getAlias());
        }
    }
}
