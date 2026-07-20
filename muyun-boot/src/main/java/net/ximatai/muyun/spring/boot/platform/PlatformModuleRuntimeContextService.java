package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.exception.AuthenticationRequiredException;
import net.ximatai.muyun.spring.common.exception.PlatformAccessDeniedException;
import net.ximatai.muyun.spring.common.exception.PlatformErrorCodes;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.platform.ActionAccessMode;
import net.ximatai.muyun.spring.common.platform.ActionAuthorizationResult;
import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.AllowAllActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.common.option.OptionSelectionMode;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicEntityDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.platform.module.ModuleEntryType;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleAction;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import net.ximatai.muyun.spring.platform.ui.PlatformPageBootstrapService;
import net.ximatai.muyun.spring.platform.ui.PlatformPageConfigSnapshot;
import net.ximatai.muyun.spring.platform.ui.PlatformPageConfigSnapshotService;
import net.ximatai.muyun.spring.platform.ui.PlatformResolvedPageConfig;
import net.ximatai.muyun.spring.platform.ui.PlatformUiClientType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class PlatformModuleRuntimeContextService {
    static final String DECISION_AUTHENTICATION_REQUIRED = "AUTHENTICATION_REQUIRED";
    static final String DECISION_ACCESS_DENIED = "ACCESS_DENIED";

    private final PlatformModuleService moduleService;
    private final PlatformModuleActionService actionService;
    private final StaticModuleDefinitionCatalog staticModuleCatalog;
    private final DynamicRecordService dynamicRecordService;
    private final ActionExecutionPolicyService actionExecutionPolicyService;
    private final PlatformPageConfigSnapshotService pageConfigSnapshotService;
    private final PlatformPageBootstrapService pageBootstrapService;

    @Autowired
    public PlatformModuleRuntimeContextService(PlatformModuleService moduleService,
                                               PlatformModuleActionService actionService,
                                               StaticModuleDefinitionCatalog staticModuleCatalog,
                                               ObjectProvider<DynamicRecordService> dynamicRecordService,
                                               ObjectProvider<PlatformPageConfigSnapshotService> pageConfigSnapshotService,
                                               ObjectProvider<PlatformPageBootstrapService> pageBootstrapService,
                                               ObjectProvider<ActionExecutionPolicyService> actionExecutionPolicyService) {
        this(moduleService, actionService, staticModuleCatalog,
                dynamicRecordService == null ? null : dynamicRecordService.getIfAvailable(),
                pageConfigSnapshotService == null ? null : pageConfigSnapshotService.getIfAvailable(),
                pageBootstrapService == null ? null : pageBootstrapService.getIfAvailable(),
                actionExecutionPolicyService == null
                        ? new AllowAllActionExecutionPolicyService()
                        : actionExecutionPolicyService.getIfAvailable(AllowAllActionExecutionPolicyService::new));
    }

    PlatformModuleRuntimeContextService(PlatformModuleService moduleService,
                                        PlatformModuleActionService actionService,
                                        StaticModuleDefinitionCatalog staticModuleCatalog,
                                        DynamicRecordService dynamicRecordService,
                                        PlatformPageConfigSnapshotService pageConfigSnapshotService,
                                        PlatformPageBootstrapService pageBootstrapService,
                                        ActionExecutionPolicyService actionExecutionPolicyService) {
        this.moduleService = moduleService;
        this.actionService = actionService;
        this.staticModuleCatalog = staticModuleCatalog;
        this.dynamicRecordService = dynamicRecordService;
        this.pageConfigSnapshotService = pageConfigSnapshotService;
        this.pageBootstrapService = pageBootstrapService;
        this.actionExecutionPolicyService = actionExecutionPolicyService == null
                ? new AllowAllActionExecutionPolicyService()
                : actionExecutionPolicyService;
    }

    public PlatformModuleRuntimeContext context(String moduleAlias) {
        String validModuleAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        PlatformModule module = moduleService.resolveVisibleModule(validModuleAlias);
        Optional<StaticModuleDefinition> staticDefinition = staticModuleCatalog.find(validModuleAlias);
        DynamicModuleDescriptor dynamicDescriptor = dynamicDescriptor(module, validModuleAlias);
        if (module == null && staticDefinition.isEmpty() && dynamicDescriptor == null) {
            throw new PlatformException(PlatformErrorCodes.RESOURCE_NOT_FOUND, 404,
                    "module runtime context not found: " + validModuleAlias);
        }
        ModuleKind moduleKind = moduleKind(module, staticDefinition, dynamicDescriptor);
        List<PlatformModuleRuntimeAction> actions = actions(validModuleAlias, moduleKind, staticDefinition,
                dynamicDescriptor);
        Set<EntityCapability> capabilities = capabilities(staticDefinition, dynamicDescriptor, actions);
        String title = title(module, staticDefinition, dynamicDescriptor, validModuleAlias);
        ResolvedModuleUiDescriptor uiDescriptor = uiDescriptor(validModuleAlias, moduleKind, title, staticDefinition,
                dynamicDescriptor);
        return new PlatformModuleRuntimeContext(
                validModuleAlias,
                title,
                moduleKind,
                entryType(module, staticDefinition),
                entryRoute(module, staticDefinition),
                entryExternalUrl(module, staticDefinition),
                mainEntityAlias(staticDefinition, dynamicDescriptor),
                capabilities,
                abilityCodes(capabilities),
                actions,
                uiDescriptor
        );
    }

    private ResolvedModuleUiDescriptor uiDescriptor(String moduleAlias,
                                                    ModuleKind moduleKind,
                                                    String title,
                                                    Optional<StaticModuleDefinition> staticDefinition,
                                                    DynamicModuleDescriptor dynamicDescriptor) {
        if (moduleKind == ModuleKind.DYNAMIC) {
            return dynamicUiDescriptor(moduleAlias, title, dynamicDescriptor);
        }
        return staticDefinition
                .map(ModuleUiDescriptorCompiler::compile)
                .orElse(null);
    }

    private ResolvedModuleUiDescriptor dynamicUiDescriptor(String moduleAlias,
                                                           String title,
                                                           DynamicModuleDescriptor dynamicDescriptor) {
        if (pageConfigSnapshotService == null || pageBootstrapService == null) {
            return null;
        }
        PlatformPageConfigSnapshot snapshot = pageConfigSnapshotService.snapshot(moduleAlias);
        PlatformResolvedPageConfig resolvedConfig = pageBootstrapService.resolveConfig(snapshot,
                PlatformUiClientType.WEB);
        ModuleUiDefinition definition = DynamicModuleUiDefinitionAdapter.fromPublishedSnapshot(snapshot,
                resolvedConfig);
        return ModuleUiDescriptorCompiler.compile(definition, ModuleKind.DYNAMIC, title,
                dynamicOptionFields(dynamicDescriptor));
    }

    private java.util.Map<String, ResolvedOptionFieldDescriptor> dynamicOptionFields(
            DynamicModuleDescriptor dynamicDescriptor) {
        if (dynamicDescriptor == null) {
            return java.util.Map.of();
        }
        return dynamicDescriptor.entities().stream()
                .filter(entity -> entity.entityAlias().equals(dynamicDescriptor.mainEntityAlias()))
                .findFirst()
                .map(entity -> entity.fields().stream()
                        .filter(field -> field.optionBinding() != null)
                        .collect(java.util.stream.Collectors.toUnmodifiableMap(
                                field -> field.fieldName(),
                                field -> new ResolvedOptionFieldDescriptor(field.optionBinding(),
                                        field.selectionMode() == null ? OptionSelectionMode.SINGLE : field.selectionMode(),
                                        null),
                                (left, right) -> left)))
                .orElseGet(java.util.Map::of);
    }

    private DynamicModuleDescriptor dynamicDescriptor(PlatformModule module, String moduleAlias) {
        if (dynamicRecordService == null) {
            if (module != null && module.getModuleKind() == ModuleKind.DYNAMIC) {
                throw new PlatformException(PlatformErrorCodes.CONFIG_MISSING, 500,
                        "dynamic record service is required for dynamic module context: " + moduleAlias);
            }
            return null;
        }
        if (module != null && module.getModuleKind() != ModuleKind.DYNAMIC) {
            return null;
        }
        try {
            return dynamicRecordService.describe(moduleAlias);
        } catch (RuntimeException ignored) {
            if (module != null && module.getModuleKind() == ModuleKind.DYNAMIC) {
                throw ignored;
            }
            return null;
        }
    }

    private ModuleKind moduleKind(PlatformModule module,
                                  Optional<StaticModuleDefinition> staticDefinition,
                                  DynamicModuleDescriptor dynamicDescriptor) {
        if (module != null && module.getModuleKind() != null) {
            return module.getModuleKind();
        }
        if (dynamicDescriptor != null) {
            return ModuleKind.DYNAMIC;
        }
        if (staticDefinition.isPresent()) {
            return ModuleKind.STATIC;
        }
        return ModuleKind.STATIC;
    }

    private String title(PlatformModule module,
                         Optional<StaticModuleDefinition> staticDefinition,
                         DynamicModuleDescriptor dynamicDescriptor,
                         String moduleAlias) {
        if (module != null && module.getTitle() != null && !module.getTitle().isBlank()) {
            return module.getTitle();
        }
        if (dynamicDescriptor != null && dynamicDescriptor.title() != null && !dynamicDescriptor.title().isBlank()) {
            return dynamicDescriptor.title();
        }
        return staticDefinition.map(StaticModuleDefinition::title).orElse(moduleAlias);
    }

    private ModuleEntryType entryType(PlatformModule module, Optional<StaticModuleDefinition> staticDefinition) {
        if (module != null && module.getEntryType() != null) {
            return module.getEntryType();
        }
        return staticDefinition.map(StaticModuleDefinition::entryType).orElse(ModuleEntryType.MODULE);
    }

    private String entryRoute(PlatformModule module, Optional<StaticModuleDefinition> staticDefinition) {
        if (module != null && module.getEntryRoute() != null) {
            return module.getEntryRoute();
        }
        return staticDefinition.map(StaticModuleDefinition::entryRoute).orElse(null);
    }

    private String entryExternalUrl(PlatformModule module, Optional<StaticModuleDefinition> staticDefinition) {
        if (module != null && module.getEntryExternalUrl() != null) {
            return module.getEntryExternalUrl();
        }
        return staticDefinition.map(StaticModuleDefinition::entryExternalUrl).orElse(null);
    }

    private String mainEntityAlias(Optional<StaticModuleDefinition> staticDefinition,
                                   DynamicModuleDescriptor dynamicDescriptor) {
        if (dynamicDescriptor != null) {
            return dynamicDescriptor.mainEntityAlias();
        }
        return staticDefinition.flatMap(definition -> definition.entities().stream()
                .findFirst()
                .map(EntityDefinition::alias)).orElse(null);
    }

    private List<PlatformModuleRuntimeAction> actions(String moduleAlias,
                                                      ModuleKind moduleKind,
                                                      Optional<StaticModuleDefinition> staticDefinition,
                                                      DynamicModuleDescriptor dynamicDescriptor) {
        List<PlatformModuleAction> persisted = actionService.listByModuleAliases(List.of(moduleAlias)).stream()
                .toList();
        if (moduleKind == ModuleKind.DYNAMIC && dynamicDescriptor != null) {
            return dynamicActions(moduleAlias, dynamicDescriptor, persisted);
        }
        List<PlatformModuleAction> enabledPersisted = persisted.stream()
                .filter(action -> Boolean.TRUE.equals(action.getEnabled()))
                .toList();
        if (!enabledPersisted.isEmpty()) {
            return enabledPersisted.stream()
                    .map(action -> runtimeAction(action, policy(action)))
                    .toList();
        }
        return staticDefinition
                .map(definition -> definition.actions().stream()
                        .map(action -> runtimeAction(definition.moduleAlias(), action))
                        .toList())
                .orElse(List.of());
    }

    private List<PlatformModuleRuntimeAction> dynamicActions(String moduleAlias,
                                                             DynamicModuleDescriptor dynamicDescriptor,
                                                             List<PlatformModuleAction> persisted) {
        LinkedHashMap<String, PlatformModuleRuntimeAction> actions = new LinkedHashMap<>();
        dynamicDescriptor.actions().stream()
                .filter(DynamicActionDescriptor::enabled)
                .forEach(action -> actions.put(action.code(), runtimeAction(moduleAlias, action)));
        for (PlatformModuleAction action : persisted) {
            if (Boolean.FALSE.equals(action.getEnabled())) {
                actions.remove(action.getActionCode());
                continue;
            }
            actions.put(action.getActionCode(), runtimeAction(action, policy(action)));
        }
        return List.copyOf(actions.values());
    }

    private PlatformModuleRuntimeAction runtimeAction(PlatformModuleAction action, ActionExecutionPolicy policy) {
        Authorization authorization = authorize(action.getModuleAlias(), policy);
        return new PlatformModuleRuntimeAction(
                action.getActionCode(),
                policy.permissionActionCode(),
                action.getTitle(),
                policy.level(),
                action.getCategory(),
                policy.accessMode(),
                policy.actionAuth(),
                policy.dataAuth(),
                policy.defaultGrantPolicy(),
                action.getExecutorType(),
                action.getExecutorKey(),
                authorization.authorized(),
                authorization.decision()
        );
    }

    private PlatformModuleRuntimeAction runtimeAction(String moduleAlias, StaticModuleActionDefinition action) {
        ActionExecutionPolicy policy = policy(action);
        Authorization authorization = authorize(moduleAlias, policy);
        return new PlatformModuleRuntimeAction(
                action.actionCode(),
                policy.permissionActionCode(),
                action.title(),
                policy.level(),
                action.category(),
                policy.accessMode(),
                policy.actionAuth(),
                policy.dataAuth(),
                policy.defaultGrantPolicy(),
                action.executorType(),
                action.executorKey(),
                authorization.authorized(),
                authorization.decision()
        );
    }

    private PlatformModuleRuntimeAction runtimeAction(String moduleAlias, DynamicActionDescriptor action) {
        ActionExecutionPolicy policy = policy(action);
        Authorization authorization = authorize(moduleAlias, policy);
        return new PlatformModuleRuntimeAction(
                action.code(),
                policy.permissionActionCode(),
                action.title(),
                policy.level(),
                action.category(),
                policy.accessMode(),
                policy.actionAuth(),
                policy.dataAuth(),
                policy.defaultGrantPolicy(),
                action.executorType(),
                action.executorKey(),
                authorization.authorized(),
                authorization.decision()
        );
    }

    private ActionExecutionPolicy policy(PlatformModuleAction action) {
        String permissionActionCode = action.getPermissionActionCode() == null || action.getPermissionActionCode().isBlank()
                ? action.getActionCode()
                : action.getPermissionActionCode();
        return new ActionExecutionPolicy(
                action.getActionCode(),
                toPlatformLevel(action.getActionLevel()),
                toAccessMode(action.getAccessMode()),
                Boolean.TRUE.equals(action.getActionAuth()),
                Boolean.TRUE.equals(action.getDataAuth()),
                action.getDefaultGrantPolicy(),
                inheritActionCode(action.getActionCode(), permissionActionCode, Boolean.TRUE.equals(action.getActionAuth()))
        );
    }

    private ActionExecutionPolicy policy(StaticModuleActionDefinition action) {
        return new ActionExecutionPolicy(
                action.actionCode(),
                toPlatformLevel(action.actionLevel()),
                toAccessMode(action.accessMode()),
                action.actionAuth(),
                action.dataAuth(),
                action.defaultGrantPolicy(),
                inheritActionCode(action.actionCode(), action.permissionActionCode(), action.actionAuth())
        );
    }

    private ActionExecutionPolicy policy(DynamicActionDescriptor action) {
        return new ActionExecutionPolicy(
                action.code(),
                toPlatformLevel(action.actionLevel()),
                toAccessMode(action.accessMode()),
                action.actionAuth(),
                action.dataAuth(),
                action.defaultGrantPolicy(),
                action.authInheritActionCode()
        );
    }

    private ActionExecutionPolicy policy(PlatformModuleRuntimeAction action) {
        return new ActionExecutionPolicy(
                action.actionCode(),
                action.actionLevel(),
                action.accessMode(),
                action.actionAuth(),
                action.dataAuth(),
                action.defaultGrantPolicy(),
                inheritActionCode(action.actionCode(), action.permissionActionCode(), action.actionAuth())
        );
    }

    private Authorization authorize(String moduleAlias, ActionExecutionPolicy policy) {
        try {
            ActionAuthorizationResult result = actionExecutionPolicyService.authorize(
                    ActionExecutionContext.ofPolicy(moduleAlias, policy, Set.of(), CurrentUserContext.currentUser()));
            return new Authorization(true, result.decision());
        } catch (AuthenticationRequiredException ignored) {
            return new Authorization(false, DECISION_AUTHENTICATION_REQUIRED);
        } catch (PlatformAccessDeniedException ignored) {
            return new Authorization(false, DECISION_ACCESS_DENIED);
        }
    }

    private Set<EntityCapability> capabilities(Optional<StaticModuleDefinition> staticDefinition,
                                               DynamicModuleDescriptor dynamicDescriptor,
                                               List<PlatformModuleRuntimeAction> actions) {
        EnumSet<EntityCapability> capabilities = baselineCapabilities();
        String staticMainEntityAlias = mainEntityAlias(staticDefinition, null);
        staticDefinition.ifPresent(definition -> {
            capabilities.addAll(definition.capabilities());
            definition.entities().stream()
                    .filter(entity -> entity.alias().equals(staticMainEntityAlias))
                    .map(EntityDefinition::capabilities)
                    .forEach(capabilities::addAll);
        });
        if (dynamicDescriptor != null) {
            for (DynamicEntityDescriptor entity : dynamicDescriptor.entities()) {
                if (!entity.entityAlias().equals(dynamicDescriptor.mainEntityAlias())) {
                    continue;
                }
                for (String capability : entity.capabilities()) {
                    capabilities.add(EntityCapability.valueOf(capability));
                }
            }
            String mainEntityAlias = dynamicDescriptor.mainEntityAlias();
            if (dynamicDescriptor.relations().stream()
                    .anyMatch(relation -> relation.parentEntityAlias().equals(mainEntityAlias))) {
                capabilities.add(EntityCapability.CHILD_RELATION);
            }
            if (dynamicDescriptor.references().stream()
                    .anyMatch(reference -> reference.sourceEntityAlias().equals(mainEntityAlias))) {
                capabilities.add(EntityCapability.REFERENCE);
                capabilities.add(EntityCapability.REFERENCE_DEPENDENCY);
            }
        }
        actions.stream()
                .map(PlatformModuleRuntimeAction::actionCode)
                .forEach(actionCode -> inferCapabilities(capabilities, actionCode));
        normalizeCapabilities(capabilities);
        return Set.copyOf(capabilities);
    }

    private Set<String> abilityCodes(Set<EntityCapability> capabilities) {
        LinkedHashSet<String> abilities = new LinkedHashSet<>();
        for (EntityCapability capability : EntityCapability.values()) {
            if (capabilities.contains(capability)) {
                abilities.add(abilityCode(capability));
            }
        }
        return Set.copyOf(abilities);
    }

    private String abilityCode(EntityCapability capability) {
        String[] parts = capability.name().toLowerCase().split("_");
        StringBuilder code = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                code.append(Character.toUpperCase(parts[i].charAt(0)));
                code.append(parts[i].substring(1));
            }
        }
        return code.toString();
    }

    private void inferCapabilities(EnumSet<EntityCapability> capabilities, String actionCode) {
        PlatformAction.fromCode(actionCode).ifPresent(action -> {
            switch (action) {
                case CREATE, VIEW, UPDATE, DELETE, BATCH_DELETE, QUERY -> capabilities.add(EntityCapability.CRUD);
                case TREE -> capabilities.add(EntityCapability.TREE);
                case SORT -> capabilities.add(EntityCapability.SORT);
                case REFERENCE -> capabilities.add(EntityCapability.REFERENCE);
                case ENABLE, DISABLE -> capabilities.add(EntityCapability.ENABLE);
                case IMPORT, EXPORT -> capabilities.add(EntityCapability.EXCHANGE);
                default -> {
                }
            }
        });
    }

    private void normalizeCapabilities(EnumSet<EntityCapability> capabilities) {
        capabilities.addAll(baselineCapabilities());
        if (capabilities.contains(EntityCapability.TREE)) {
            capabilities.add(EntityCapability.SORT);
        }
        if (capabilities.contains(EntityCapability.APPROVAL)) {
            capabilities.add(EntityCapability.WORKFLOW);
        }
    }

    private EnumSet<EntityCapability> baselineCapabilities() {
        EnumSet<EntityCapability> capabilities = EnumSet.noneOf(EntityCapability.class);
        for (EntityCapability capability : EntityCapability.values()) {
            if (capability.isBaseline()) {
                capabilities.add(capability);
            }
        }
        return capabilities;
    }

    static String inheritActionCode(String actionCode, String permissionActionCode, boolean actionAuth) {
        if (!actionAuth || permissionActionCode == null || permissionActionCode.isBlank()
                || actionCode.equals(permissionActionCode)) {
            return null;
        }
        return permissionActionCode;
    }

    private PlatformActionLevel toPlatformLevel(EntityActionLevel level) {
        if (level == null) {
            return PlatformActionLevel.ANY;
        }
        return switch (level) {
            case LIST -> PlatformActionLevel.LIST;
            case RECORD -> PlatformActionLevel.RECORD;
            case BATCH -> PlatformActionLevel.BATCH;
            case ANY -> PlatformActionLevel.ANY;
        };
    }

    private ActionAccessMode toAccessMode(EntityActionAccessMode accessMode) {
        if (accessMode == null) {
            return ActionAccessMode.AUTH_REQUIRED;
        }
        return ActionAccessMode.valueOf(accessMode.name());
    }

    private record Authorization(boolean authorized, String decision) {
    }
}
