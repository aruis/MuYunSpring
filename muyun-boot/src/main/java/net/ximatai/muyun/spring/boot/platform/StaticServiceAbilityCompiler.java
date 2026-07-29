package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.ability.CacheAbility;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.DataScopeAbility;
import net.ximatai.muyun.spring.ability.DisablePlatformOperations;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.PlatformOperation;
import net.ximatai.muyun.spring.ability.RecycleBinAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.ability.child.ChildrenAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.ability.reference.ReferencerAbility;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Compiles static service interface composition into the shared platform capability vocabulary. */
public final class StaticServiceAbilityCompiler {
    private static final Set<EntityCapability> SERVICE_DECLARED_CAPABILITIES = Set.of(
            EntityCapability.CRUD,
            EntityCapability.SOFT_DELETE,
            EntityCapability.CACHE,
            EntityCapability.TREE,
            EntityCapability.SORT,
            EntityCapability.REFERENCE,
            EntityCapability.ENABLE,
            EntityCapability.DATA_SCOPE,
            EntityCapability.RECYCLE_BIN,
            EntityCapability.CHILD_RELATION,
            EntityCapability.REFERENCE_DEPENDENCY
    );

    private StaticServiceAbilityCompiler() {
    }

    static Set<EntityCapability> compile(Object service) {
        EnumSet<EntityCapability> capabilities = EnumSet.noneOf(EntityCapability.class);
        if (service instanceof CrudAbility<?>) capabilities.add(EntityCapability.CRUD);
        if (service instanceof SoftDeleteAbility<?>) capabilities.add(EntityCapability.SOFT_DELETE);
        if (service instanceof CacheAbility<?>) capabilities.add(EntityCapability.CACHE);
        if (service instanceof TreeAbility<?>) {
            capabilities.add(EntityCapability.TREE);
            capabilities.add(EntityCapability.SORT);
        } else if (service instanceof SortAbility<?>) {
            capabilities.add(EntityCapability.SORT);
        }
        if (service instanceof ReferenceAbility<?>) capabilities.add(EntityCapability.REFERENCE);
        if (service instanceof ReferencerAbility<?>) capabilities.add(EntityCapability.REFERENCE_DEPENDENCY);
        if (service instanceof EnableAbility<?>) capabilities.add(EntityCapability.ENABLE);
        if (service instanceof DataScopeAbility<?>) capabilities.add(EntityCapability.DATA_SCOPE);
        if (service instanceof RecycleBinAbility<?>) capabilities.add(EntityCapability.RECYCLE_BIN);
        if (service instanceof ChildrenAbility<?>) capabilities.add(EntityCapability.CHILD_RELATION);
        return Set.copyOf(capabilities);
    }

    static boolean isServiceDeclared(EntityCapability capability) {
        return SERVICE_DECLARED_CAPABILITIES.contains(capability);
    }

    static List<PlatformAction> standardActions(Object service) {
        List<PlatformAction> actions = new ArrayList<>();
        if (service instanceof TreeAbility<?>) {
            actions.add(PlatformAction.TREE);
            actions.add(PlatformAction.SORT);
        } else if (service instanceof SortAbility<?>) {
            actions.add(PlatformAction.SORT);
        }
        if (service instanceof EnableAbility<?>) {
            Set<PlatformAction> directOperations = operationMethods(service).keySet();
            if (directOperations.contains(PlatformAction.ENABLE)) {
                actions.add(PlatformAction.ENABLE);
            }
            if (directOperations.contains(PlatformAction.DISABLE)) {
                actions.add(PlatformAction.DISABLE);
            }
        }
        if (service instanceof RecycleBinAbility<?> recycleBinAbility) {
            actions.add(PlatformAction.RECYCLE_BIN_QUERY);
            actions.add(PlatformAction.RECYCLE_BIN_RESTORE);
            if (recycleBinAbility.isRecycleBinPurgeEnabled()) {
                actions.add(PlatformAction.RECYCLE_BIN_PURGE);
            }
        }
        Set<PlatformAction> disabled = disabledActions(service);
        return actions.stream().filter(action -> !disabled.contains(action)).toList();
    }

    public static Map<PlatformAction, Method> operationMethods(Object service) {
        if (service == null) {
            return Map.of();
        }
        Set<PlatformAction> disabled = disabledActions(service);
        LinkedHashMap<PlatformAction, Method> methods = new LinkedHashMap<>();
        for (Class<?> serviceInterface : allInterfaces(AopUtils.getTargetClass(service))) {
            for (Method method : serviceInterface.getDeclaredMethods()) {
                PlatformOperation operation = method.getAnnotation(PlatformOperation.class);
                if (operation == null || disabled.contains(operation.value())) {
                    continue;
                }
                Method existing = methods.putIfAbsent(operation.value(), method);
                if (existing != null && !existing.equals(method)) {
                    throw new IllegalStateException("duplicate platform operation " + operation.value().code()
                            + " on " + service.getClass().getName() + ": " + existing + " and " + method);
                }
            }
        }
        return Map.copyOf(methods);
    }

    public static Set<PlatformAction> disabledActions(Object service) {
        if (service == null) {
            return Set.of();
        }
        Class<?> serviceClass = AopUtils.getTargetClass(service);
        DisablePlatformOperations annotation = serviceClass.getDeclaredAnnotation(DisablePlatformOperations.class);
        if (annotation == null || annotation.value().length == 0) {
            return Set.of();
        }
        EnumSet<PlatformAction> actions = EnumSet.noneOf(PlatformAction.class);
        java.util.Collections.addAll(actions, annotation.value());
        return Set.copyOf(actions);
    }

    private static Set<Class<?>> allInterfaces(Class<?> type) {
        LinkedHashSet<Class<?>> interfaces = new LinkedHashSet<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Class<?> serviceInterface : current.getInterfaces()) {
                collectInterface(serviceInterface, interfaces);
            }
            current = current.getSuperclass();
        }
        return interfaces;
    }

    private static void collectInterface(Class<?> type, Set<Class<?>> interfaces) {
        if (!interfaces.add(type)) {
            return;
        }
        for (Class<?> parent : type.getInterfaces()) {
            collectInterface(parent, interfaces);
        }
    }
}
