package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleAction;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StaticModuleDefinitionRegistrar implements ApplicationRunner {
    private final PlatformModuleService moduleService;
    private final PlatformModuleActionService actionService;
    private final List<StaticModuleDefinition> definitions;
    private final List<StaticModuleDefinitionScanner> scanners;

    public StaticModuleDefinitionRegistrar(PlatformModuleService moduleService,
                                           PlatformModuleActionService actionService,
                                           List<StaticModuleDefinition> definitions) {
        this(moduleService, actionService, definitions, List.of());
    }

    public StaticModuleDefinitionRegistrar(PlatformModuleService moduleService,
                                           PlatformModuleActionService actionService,
                                           List<StaticModuleDefinition> definitions,
                                           List<StaticModuleDefinitionScanner> scanners) {
        this.moduleService = moduleService;
        this.actionService = actionService;
        this.definitions = definitions == null ? List.of() : List.copyOf(definitions);
        this.scanners = scanners == null ? List.of() : List.copyOf(scanners);
    }

    @Override
    public void run(ApplicationArguments args) {
        registerAll();
    }

    public void registerAll() {
        try (TenantContext.Scope ignored = TenantContext.system("register static modules")) {
            for (StaticModuleDefinition definition : allDefinitions()) {
                registerModule(definition);
                registerActions(definition);
            }
        }
    }

    private List<StaticModuleDefinition> allDefinitions() {
        if (scanners.isEmpty()) {
            validateDefinitions(definitions);
            return definitions;
        }
        ArrayList<StaticModuleDefinition> all = new ArrayList<>(definitions);
        for (StaticModuleDefinitionScanner scanner : scanners) {
            all.addAll(scanner.scan());
        }
        validateDefinitions(all);
        return List.copyOf(all);
    }

    private void validateDefinitions(List<StaticModuleDefinition> definitions) {
        Set<String> modules = new HashSet<>();
        for (StaticModuleDefinition definition : definitions) {
            if (!modules.add(definition.moduleAlias())) {
                throw new IllegalStateException("duplicate static module definition: " + definition.moduleAlias());
            }
            Set<String> actions = new HashSet<>();
            for (StaticModuleActionDefinition action : definition.actions()) {
                if (!actions.add(action.actionCode())) {
                    throw new IllegalStateException("duplicate static module action definition: "
                            + definition.moduleAlias() + "." + action.actionCode());
                }
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
            action.setActionLevel(actionDefinition.actionLevel());
            action.setAccessMode(actionDefinition.accessMode());
            action.setActionAuth(actionDefinition.actionAuth());
            action.setDataAuth(actionDefinition.dataAuth());
            action.setDefaultGrantPolicy(actionDefinition.defaultGrantPolicy());
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
}
