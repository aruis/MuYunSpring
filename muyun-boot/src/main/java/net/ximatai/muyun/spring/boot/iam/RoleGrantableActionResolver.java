package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.iam.role.GrantableAction;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataAction;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataActionService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelationService;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import net.ximatai.muyun.spring.platform.module.PlatformModuleAction;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Component
public class RoleGrantableActionResolver {
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final PlatformModuleService moduleService;
    private final ModuleMetadataRelationService relationService;
    private final ModuleMetadataActionService actionService;
    private final PlatformModuleActionService moduleActionService;
    private final StaticModuleActionRegistry staticModuleActionRegistry;

    public RoleGrantableActionResolver(PlatformModuleService moduleService,
                                       ModuleMetadataRelationService relationService,
                                       ModuleMetadataActionService actionService) {
        this(moduleService, relationService, actionService, null, new StaticModuleActionRegistry());
    }

    public RoleGrantableActionResolver(PlatformModuleService moduleService,
                                       ModuleMetadataRelationService relationService,
                                       ModuleMetadataActionService actionService,
                                       StaticModuleActionRegistry staticModuleActionRegistry) {
        this(moduleService, relationService, actionService, null, staticModuleActionRegistry);
    }

    @Autowired
    public RoleGrantableActionResolver(PlatformModuleService moduleService,
                                       ModuleMetadataRelationService relationService,
                                       ModuleMetadataActionService actionService,
                                       PlatformModuleActionService moduleActionService,
                                       StaticModuleActionRegistry staticModuleActionRegistry) {
        this.moduleService = moduleService;
        this.relationService = relationService;
        this.actionService = actionService;
        this.moduleActionService = moduleActionService;
        this.staticModuleActionRegistry = staticModuleActionRegistry == null
                ? new StaticModuleActionRegistry()
                : staticModuleActionRegistry;
    }

    public List<GrantableAction> resolve(List<String> moduleAliases) {
        if (moduleAliases == null || moduleAliases.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<String, GrantableAction> actions = new LinkedHashMap<>();
        for (String moduleAlias : moduleAliases.stream().filter(java.util.Objects::nonNull).distinct().toList()) {
            PlatformModule module = moduleService.resolveVisibleModule(moduleAlias);
            if (module == null) {
                continue;
            }
            List<GrantableAction> registeredActions = registeredModuleActions(moduleAlias);
            if (!registeredActions.isEmpty()) {
                for (GrantableAction action : registeredActions) {
                    put(actions, action);
                }
            } else if (module.getModuleKind() == null || module.getModuleKind() == ModuleKind.STATIC) {
                for (PlatformAction action : staticModuleActionRegistry.grantableActions(moduleAlias)) {
                    put(actions, GrantableAction.ofPlatformDefaults(moduleAlias, action));
                }
            }
            for (GrantableAction action : configuredActions(moduleAlias)) {
                put(actions, action);
            }
        }
        return List.copyOf(actions.values());
    }

    private List<GrantableAction> registeredModuleActions(String moduleAlias) {
        if (moduleActionService == null) {
            return List.of();
        }
        return moduleActionService.listByModuleAliases(List.of(moduleAlias)).stream()
                .filter(action -> Boolean.TRUE.equals(action.getEnabled()))
                .filter(action -> action.getActionAuth() == null || Boolean.TRUE.equals(action.getActionAuth()))
                .map(action -> toGrantableAction(moduleAlias, action))
                .toList();
    }

    private GrantableAction toGrantableAction(String moduleAlias, PlatformModuleAction action) {
        return new GrantableAction(
                moduleAlias,
                action.getActionCode(),
                action.getPermissionActionCode(),
                action.getTitle(),
                action.getActionAuth() == null || Boolean.TRUE.equals(action.getActionAuth()),
                Boolean.TRUE.equals(action.getDataAuth())
        );
    }

    private List<GrantableAction> configuredActions(String moduleAlias) {
        List<String> relationIds = relationService.list(Criteria.of().eq("moduleAlias", moduleAlias), ALL)
                .stream()
                .map(ModuleMetadataRelation::getId)
                .toList();
        if (relationIds.isEmpty()) {
            return List.of();
        }
        List<GrantableAction> actions = new ArrayList<>();
        for (ModuleMetadataAction action : actionService.listByRelationIds(relationIds)) {
            if (!Boolean.TRUE.equals(action.getEnabled()) || Boolean.FALSE.equals(action.getActionAuth())) {
                continue;
            }
            actions.add(new GrantableAction(
                    moduleAlias,
                    action.getActionCode(),
                    permissionActionCode(action),
                    action.getTitle(),
                    true,
                    Boolean.TRUE.equals(action.getDataAuth())
            ));
        }
        return actions;
    }

    private String permissionActionCode(ModuleMetadataAction action) {
        String inherited = action.getAuthInheritActionCode();
        if (inherited != null && !inherited.isBlank()) {
            return PlatformAction.permissionActionCodeOf(inherited);
        }
        return PlatformAction.permissionActionCodeOf(action.getActionCode());
    }

    private void put(LinkedHashMap<String, GrantableAction> actions, GrantableAction action) {
        String key = action.moduleAlias() + ":" + action.permissionActionCode();
        GrantableAction existing = actions.get(key);
        if (existing == null || action.actionCode().equals(action.permissionActionCode())) {
            actions.put(key, action);
        }
    }
}
