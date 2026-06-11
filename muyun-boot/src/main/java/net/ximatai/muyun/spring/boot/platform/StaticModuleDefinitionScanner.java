package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.ReadOnlyWeb;
import net.ximatai.muyun.spring.boot.web.ReferenceWeb;
import net.ximatai.muyun.spring.boot.web.ScopedWeb;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.boot.web.TreeWeb;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StaticModuleDefinitionScanner {
    private final ApplicationContext applicationContext;

    public StaticModuleDefinitionScanner(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public List<StaticModuleDefinition> scan() {
        List<StaticModuleDefinition> definitions = new ArrayList<>();
        for (String beanName : applicationContext.getBeanNamesForAnnotation(PlatformStaticModule.class)) {
            Object bean = applicationContext.getBean(beanName);
            Class<?> beanClass = AopUtils.getTargetClass(bean);
            PlatformStaticModule module = AnnotationUtils.findAnnotation(beanClass, PlatformStaticModule.class);
            if (module == null) {
                continue;
            }
            definitions.add(definition(beanClass, module));
        }
        return List.copyOf(definitions);
    }

    private StaticModuleDefinition definition(Class<?> beanClass, PlatformStaticModule module) {
        validateScopeAlias(beanClass, module);
        return new StaticModuleDefinition(
                module.application(),
                module.alias(),
                module.title(),
                module.parent().isBlank() ? null : module.parent(),
                java.util.Set.of(module.capabilities()),
                actions(beanClass, java.util.Set.of(module.capabilities()))
        );
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
        addStandardActions(actions, beanClass);
        addWorkflowActions(actions, capabilities);
        ReflectionUtils.doWithMethods(beanClass, method -> addAnnotatedAction(actions, method));
        return List.copyOf(actions.values());
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

    private void addPlatform(Map<String, StaticModuleActionDefinition> actions, PlatformAction action) {
        actions.putIfAbsent(action.code(), StaticModuleActionDefinition.platformAction(action));
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
