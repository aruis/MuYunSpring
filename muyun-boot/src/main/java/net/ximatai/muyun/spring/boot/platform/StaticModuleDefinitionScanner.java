package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.ReadOnlyWeb;
import net.ximatai.muyun.spring.boot.web.ReferenceWeb;
import net.ximatai.muyun.spring.boot.web.ScopedWeb;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.boot.web.TreeWeb;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.StaticEntityDefinitionCompiler;
import net.ximatai.muyun.spring.platform.module.ModuleEntryType;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StaticModuleDefinitionScanner {
    private final ApplicationContext applicationContext;

    public StaticModuleDefinitionScanner(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public List<StaticModuleDefinition> scan() {
        LinkedHashMap<String, StaticModuleDefinition> definitions = new LinkedHashMap<>();
        for (String beanName : applicationContext.getBeanNamesForAnnotation(PlatformStaticModule.class)) {
            Object bean = applicationContext.getBean(beanName);
            Class<?> beanClass = AopUtils.getTargetClass(bean);
            PlatformStaticModule module = AnnotationUtils.findAnnotation(beanClass, PlatformStaticModule.class);
            if (module == null) {
                continue;
            }
            StaticModuleDefinition definition = definition(bean, beanClass, module);
            definitions.put(definition.moduleAlias(), definition);
        }
        addActionContributions(definitions);
        return List.copyOf(definitions.values());
    }

    private StaticModuleDefinition definition(Object bean, Class<?> beanClass, PlatformStaticModule module) {
        validateScopeAlias(beanClass, module);
        List<RelationProjectionJoinDefinition> projectionJoins = projectionJoins(bean);
        return new StaticModuleDefinition(
                module.application(),
                module.alias(),
                module.title(),
                module.parent().isBlank() ? null : module.parent(),
                entryType(module),
                module.route(),
                module.externalUrl(),
                java.util.Set.of(module.capabilities()),
                actions(beanClass, java.util.Set.of(module.capabilities())),
                entities(bean, module, projectionJoins),
                uiDefinition(bean, module),
                projectionJoins
        );
    }

    private List<RelationProjectionJoinDefinition> projectionJoins(Object bean) {
        if (!(bean instanceof RelationProjectionJoinContributor contributor)) {
            return List.of();
        }
        List<RelationProjectionJoinDefinition> joins = contributor.projectionJoins();
        return joins == null ? List.of() : List.copyOf(joins);
    }

    private ModuleUiDefinition uiDefinition(Object bean, PlatformStaticModule module) {
        if (!(bean instanceof StaticModuleUiContributor contributor)) {
            return null;
        }
        ModuleUiDefinition uiDefinition = contributor.moduleUiDefinition();
        if (uiDefinition == null) {
            return null;
        }
        if (!module.alias().equals(uiDefinition.moduleAlias())) {
            throw new IllegalStateException("static module UI definition alias must match module alias: "
                    + module.alias() + " != " + uiDefinition.moduleAlias());
        }
        return uiDefinition;
    }

    private ModuleEntryType entryType(PlatformStaticModule module) {
        boolean hasRoute = !module.route().isBlank();
        boolean hasExternalUrl = !module.externalUrl().isBlank();
        if (hasRoute && hasExternalUrl) {
            throw new IllegalStateException("@PlatformStaticModule cannot declare both route and externalUrl: "
                    + module.alias());
        }
        if (hasRoute) {
            return ModuleEntryType.ROUTE;
        }
        if (hasExternalUrl) {
            return ModuleEntryType.LINK;
        }
        return ModuleEntryType.MODULE;
    }

    private List<EntityDefinition> entities(Object bean,
                                            PlatformStaticModule module,
                                            List<RelationProjectionJoinDefinition> projectionJoins) {
        Object service = service(bean);
        if (!(service instanceof CrudAbility<?> ability)) {
            return List.of();
        }
        Class<?> modelClass = ability.modelClass();
        if (modelClass == null || modelClass == Object.class) {
            return List.of();
        }
        LinkedHashMap<String, EntityDefinition> entities = new LinkedHashMap<>();
        EntityDefinition mainEntity = new StaticEntityDefinitionCompiler().compile(
                entityAlias(module),
                module.title(),
                modelClass
        );
        entities.put(mainEntity.alias(), mainEntity);
        for (RelationProjectionJoinDefinition join : projectionJoins) {
            EntityDefinition target = join.targetEntity();
            if (entities.containsKey(target.alias())) {
                throw new IllegalStateException("static projection join entity conflicts with module entity: "
                        + module.alias() + "." + target.alias());
            }
            entities.put(target.alias(), target);
        }
        return List.copyOf(entities.values());
    }

    private String entityAlias(PlatformStaticModule module) {
        String moduleName = module.alias().substring(module.application().length() + 1);
        int lastSeparator = moduleName.lastIndexOf('.');
        if (lastSeparator < 0) {
            return moduleName;
        }
        return moduleName.substring(lastSeparator + 1);
    }

    private Object service(Object bean) {
        if (!(bean instanceof ScopedWeb<?> scopedWeb)) {
            return null;
        }
        return scopedWeb.service();
    }

    private void validateScopeAlias(Class<?> beanClass, PlatformStaticModule module) {
        if (!ScopedWeb.class.isAssignableFrom(beanClass)) {
            return;
        }
        org.springframework.web.bind.annotation.RequestMapping mapping =
                AnnotationUtils.findAnnotation(beanClass, org.springframework.web.bind.annotation.RequestMapping.class);
        String path = mapping == null ? null : firstText(mapping.value());
        if (path == null && mapping != null) {
            path = firstText(mapping.path());
        }
        if (path == null) {
            return;
        }
        String scopeName = path.replaceFirst("^/", "");
        if (scopeName.contains("/")) {
            return;
        }
        if (!module.alias().equals(scopeName) && !normalizeScope(module.alias()).equals(normalizeScope(scopeName))) {
            throw new IllegalStateException("@PlatformStaticModule alias must match web scope: "
                    + module.alias() + " != " + scopeName);
        }
    }

    private String normalizeScope(String value) {
        return value == null ? "" : value.replace("-", "_").toLowerCase(java.util.Locale.ROOT);
    }

    private List<StaticModuleActionDefinition> actions(Class<?> beanClass,
                                                       java.util.Set<EntityCapability> capabilities) {
        LinkedHashMap<String, StaticModuleActionDefinition> actions = new LinkedHashMap<>();
        addMenuAction(actions, beanClass);
        addStandardActions(actions, beanClass);
        addWorkflowActions(actions, capabilities);
        ReflectionUtils.doWithMethods(beanClass, method -> addAnnotatedAction(actions, method));
        return List.copyOf(actions.values());
    }

    private void addActionContributions(LinkedHashMap<String, StaticModuleDefinition> definitions) {
        for (String beanName : applicationContext.getBeanNamesForAnnotation(PlatformStaticActionContribution.class)) {
            Object bean = applicationContext.getBean(beanName);
            Class<?> beanClass = AopUtils.getTargetClass(bean);
            PlatformStaticActionContribution contribution =
                    AnnotationUtils.findAnnotation(beanClass, PlatformStaticActionContribution.class);
            if (contribution == null) {
                continue;
            }
            String targetModule = PlatformStaticActionContributionSupport.targetModule(contribution);
            StaticModuleDefinition target = definitions.get(targetModule);
            if (target == null) {
                throw new IllegalStateException("@PlatformStaticActionContribution target module is not scanned: "
                        + targetModule + " <- " + beanClass.getName());
            }
            LinkedHashMap<String, StaticModuleActionDefinition> merged = new LinkedHashMap<>();
            target.actions().forEach(action -> merged.put(action.actionCode(), action));
            contributionActions(beanClass, contribution)
                    .forEach(action -> mergeContributionAction(target.moduleAlias(), beanClass, merged, action));
            List<EntityDefinition> entities = mergeContributionEntities(
                    target.moduleAlias(), beanClass, target.entities(), contributionEntities(bean, contribution));
            ModuleUiDefinition uiDefinition = mergeContributionUiDefinition(
                    target.moduleAlias(),
                    beanClass,
                    target.uiDefinition(),
                    contributionUiDefinition(bean, targetModule)
            );
            definitions.put(targetModule, new StaticModuleDefinition(
                    target.applicationAlias(),
                    target.moduleAlias(),
                    target.title(),
                    target.parentModuleAlias(),
                    target.entryType(),
                    target.entryRoute(),
                    target.entryExternalUrl(),
                    target.capabilities(),
                    List.copyOf(merged.values()),
                    entities,
                    uiDefinition,
                    target.projectionJoins()
            ));
        }
    }

    private List<EntityDefinition> contributionEntities(Object bean,
                                                        PlatformStaticActionContribution contribution) {
        Object service = service(bean);
        if (!(service instanceof CrudAbility<?> ability)) {
            return List.of();
        }
        Class<?> modelClass = ability.modelClass();
        if (modelClass == null || modelClass == Object.class) {
            return List.of();
        }
        return List.of(new StaticEntityDefinitionCompiler().compile(
                contribution.resource(),
                contribution.resourceTitle(),
                modelClass
        ));
    }

    private List<EntityDefinition> mergeContributionEntities(String targetModule,
                                                             Class<?> contributor,
                                                             List<EntityDefinition> targetEntities,
                                                             List<EntityDefinition> contributionEntities) {
        LinkedHashMap<String, EntityDefinition> merged = new LinkedHashMap<>();
        for (EntityDefinition entity : targetEntities) {
            merged.put(entity.alias(), entity);
        }
        for (EntityDefinition entity : contributionEntities) {
            if (merged.containsKey(entity.alias())) {
                throw new IllegalStateException("@PlatformStaticActionContribution entity conflicts with target module: "
                        + targetModule + "." + entity.alias() + " <- " + contributor.getName());
            }
            merged.put(entity.alias(), entity);
        }
        return List.copyOf(merged.values());
    }

    private ModuleUiDefinition contributionUiDefinition(Object bean, String targetModule) {
        if (!(bean instanceof StaticModuleUiContributor contributor)) {
            return null;
        }
        ModuleUiDefinition uiDefinition = contributor.moduleUiDefinition();
        if (uiDefinition == null) {
            return null;
        }
        if (!targetModule.equals(uiDefinition.moduleAlias())) {
            throw new IllegalStateException("@PlatformStaticActionContribution UI definition alias must match target module: "
                    + targetModule + " != " + uiDefinition.moduleAlias());
        }
        return uiDefinition;
    }

    private ModuleUiDefinition mergeContributionUiDefinition(String targetModule,
                                                             Class<?> contributor,
                                                             ModuleUiDefinition targetUiDefinition,
                                                             ModuleUiDefinition contributionUiDefinition) {
        if (contributionUiDefinition == null) {
            return targetUiDefinition;
        }
        LinkedHashMap<String, ViewDefinition> views = new LinkedHashMap<>();
        if (targetUiDefinition != null) {
            targetUiDefinition.views().forEach(view -> views.put(view.viewCode(), view));
        }
        for (ViewDefinition view : contributionUiDefinition.views()) {
            if (views.containsKey(view.viewCode())) {
                throw new IllegalStateException("@PlatformStaticActionContribution UI view conflicts with target module: "
                        + targetModule + "." + view.viewCode() + " <- " + contributor.getName());
            }
            views.put(view.viewCode(), view);
        }
        return new ModuleUiDefinition(targetModule, List.copyOf(views.values()));
    }

    private void mergeContributionAction(String targetModule,
                                         Class<?> contributor,
                                         LinkedHashMap<String, StaticModuleActionDefinition> actions,
                                         StaticModuleActionDefinition action) {
        if (actions.containsKey(action.actionCode())) {
            throw new IllegalStateException("@PlatformStaticActionContribution action conflicts with target module: "
                    + targetModule + "." + action.actionCode() + " <- " + contributor.getName());
        }
        actions.put(action.actionCode(), action);
    }

    private List<StaticModuleActionDefinition> contributionActions(Class<?> beanClass,
                                                                   PlatformStaticActionContribution contribution) {
        LinkedHashMap<String, StaticModuleActionDefinition> actions = new LinkedHashMap<>();
        addContributionStandardActions(actions, beanClass, contribution);
        ReflectionUtils.doWithMethods(beanClass, method -> addContributionAnnotatedAction(actions, method,
                contribution));
        return List.copyOf(actions.values());
    }

    private void addMenuAction(Map<String, StaticModuleActionDefinition> actions, Class<?> beanClass) {
        if (AnnotationUtils.findAnnotation(beanClass, PlatformMenu.class) != null) {
            addPlatform(actions, PlatformAction.MENU);
        }
    }

    private void addStandardActions(Map<String, StaticModuleActionDefinition> actions, Class<?> beanClass) {
        if (CrudWeb.class.isAssignableFrom(beanClass)) {
            addPlatform(actions, PlatformAction.MENU);
            addPlatform(actions, PlatformAction.CREATE);
            addPlatform(actions, PlatformAction.VIEW);
            addPlatform(actions, PlatformAction.UPDATE);
            addPlatform(actions, PlatformAction.DELETE);
            addPlatform(actions, PlatformAction.QUERY);
        } else if (ReadOnlyWeb.class.isAssignableFrom(beanClass)) {
            addPlatform(actions, PlatformAction.MENU);
            addPlatform(actions, PlatformAction.VIEW);
            addPlatform(actions, PlatformAction.QUERY);
        }
        if (TreeWeb.class.isAssignableFrom(beanClass)) {
            addPlatform(actions, PlatformAction.TREE);
            addPlatform(actions, PlatformAction.SORT);
        } else if (SortWeb.class.isAssignableFrom(beanClass)) {
            addPlatform(actions, PlatformAction.SORT);
        }
        if (EnableWeb.class.isAssignableFrom(beanClass)) {
            addPlatform(actions, PlatformAction.ENABLE);
            addPlatform(actions, PlatformAction.DISABLE);
        }
        if (ReferenceWeb.class.isAssignableFrom(beanClass)) {
            addPlatform(actions, PlatformAction.REFERENCE);
        }
    }

    private void addContributionStandardActions(Map<String, StaticModuleActionDefinition> actions,
                                                Class<?> beanClass,
                                                PlatformStaticActionContribution contribution) {
        if (CrudWeb.class.isAssignableFrom(beanClass)) {
            addContributionPlatform(actions, contribution, PlatformAction.CREATE);
            addContributionPlatform(actions, contribution, PlatformAction.VIEW);
            addContributionPlatform(actions, contribution, PlatformAction.UPDATE);
            addContributionPlatform(actions, contribution, PlatformAction.DELETE);
            addContributionPlatform(actions, contribution, PlatformAction.QUERY);
        } else if (ReadOnlyWeb.class.isAssignableFrom(beanClass)) {
            addContributionPlatform(actions, contribution, PlatformAction.VIEW);
            addContributionPlatform(actions, contribution, PlatformAction.QUERY);
        }
        if (TreeWeb.class.isAssignableFrom(beanClass)) {
            addContributionPlatform(actions, contribution, PlatformAction.TREE);
            addContributionPlatform(actions, contribution, PlatformAction.SORT);
        } else if (SortWeb.class.isAssignableFrom(beanClass)) {
            addContributionPlatform(actions, contribution, PlatformAction.SORT);
        }
        if (EnableWeb.class.isAssignableFrom(beanClass)) {
            addContributionPlatform(actions, contribution, PlatformAction.ENABLE);
            addContributionPlatform(actions, contribution, PlatformAction.DISABLE);
        }
        if (ReferenceWeb.class.isAssignableFrom(beanClass)) {
            addContributionPlatform(actions, contribution, PlatformAction.REFERENCE);
        }
    }

    private void addWorkflowActions(Map<String, StaticModuleActionDefinition> actions,
                                    java.util.Set<EntityCapability> capabilities) {
        if (capabilities == null || capabilities.isEmpty()) {
            return;
        }
        if (capabilities.contains(EntityCapability.APPROVAL)) {
            actions.putIfAbsent("submitApproval",
                    StaticModuleActionDefinition.workflowAction("submitApproval", "提交审批"));
        }
    }

    private void addAnnotatedAction(Map<String, StaticModuleActionDefinition> actions, Method method) {
        ActionEndpoint standard = AnnotationUtils.findAnnotation(method, ActionEndpoint.class);
        if (standard != null) {
            addPlatform(actions, standard.value());
        }
        CustomActionEndpoint custom = AnnotationUtils.findAnnotation(method, CustomActionEndpoint.class);
        if (custom != null) {
            actions.put(custom.value(), new StaticModuleActionDefinition(
                    custom.value(),
                    custom.value(),
                    custom.title().isBlank() ? custom.value() : custom.title(),
                    toEntityLevel(custom.level()),
                    net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode.AUTH_REQUIRED,
                    true,
                    custom.dataAuth(),
                    net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy.NONE
            ));
        }
    }

    private void addContributionAnnotatedAction(Map<String, StaticModuleActionDefinition> actions,
                                                Method method,
                                                PlatformStaticActionContribution contribution) {
        ActionEndpoint standard = AnnotationUtils.findAnnotation(method, ActionEndpoint.class);
        if (standard != null) {
            addContributionPlatform(actions, contribution, standard.value());
        }
        CustomActionEndpoint custom = AnnotationUtils.findAnnotation(method, CustomActionEndpoint.class);
        if (custom != null) {
            String actionCode = PlatformStaticActionContributionSupport.actionCode(contribution, custom.value());
            actions.put(actionCode, new StaticModuleActionDefinition(
                    actionCode,
                    actionCode,
                    PlatformStaticActionContributionSupport.title(contribution,
                            custom.title().isBlank() ? custom.value() : custom.title()),
                    toEntityLevel(custom.level()),
                    net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode.AUTH_REQUIRED,
                    true,
                    custom.dataAuth(),
                    net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy.NONE
            ));
        }
    }

    private void addPlatform(Map<String, StaticModuleActionDefinition> actions, PlatformAction action) {
        actions.putIfAbsent(action.code(), StaticModuleActionDefinition.platformAction(action));
    }

    private void addContributionPlatform(Map<String, StaticModuleActionDefinition> actions,
                                         PlatformStaticActionContribution contribution,
                                         PlatformAction action) {
        if (action == PlatformAction.MENU) {
            return;
        }
        String actionCode = PlatformStaticActionContributionSupport.actionCode(contribution, action);
        actions.putIfAbsent(actionCode, new StaticModuleActionDefinition(
                actionCode,
                PlatformStaticActionContributionSupport.permissionActionCode(contribution, action),
                PlatformStaticActionContributionSupport.title(contribution, action),
                toEntityLevel(action.level()),
                net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode.valueOf(action.accessMode().name()),
                action.actionAuth(),
                action.dataAuth(),
                action.defaultGrantPolicy()
        ));
    }

    private EntityActionLevel toEntityLevel(net.ximatai.muyun.spring.common.platform.PlatformActionLevel level) {
        if (level == null) {
            return EntityActionLevel.ANY;
        }
        return switch (level) {
            case LIST -> EntityActionLevel.LIST;
            case RECORD -> EntityActionLevel.RECORD;
            case BATCH -> EntityActionLevel.BATCH;
            case DEFAULT, ANY -> EntityActionLevel.ANY;
        };
    }

    private String firstText(String[] values) {
        if (values == null || values.length == 0 || values[0].isBlank()) {
            return null;
        }
        return values[0];
    }
}
