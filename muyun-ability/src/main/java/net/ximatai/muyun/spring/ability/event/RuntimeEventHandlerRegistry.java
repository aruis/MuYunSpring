package net.ximatai.muyun.spring.ability.event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class RuntimeEventHandlerRegistry {
    private final List<RuntimeEventHandlerDescriptor> descriptors;

    public RuntimeEventHandlerRegistry(List<RuntimeEventHandlerDescriptor> descriptors) {
        List<RuntimeEventHandlerDescriptor> ordered = descriptors == null ? new ArrayList<>() : new ArrayList<>(descriptors);
        validateUnique(ordered);
        ordered.sort(Comparator
                .comparingInt(RuntimeEventHandlerDescriptor::order)
                .thenComparing(RuntimeEventHandlerDescriptor::moduleAlias)
                .thenComparing(descriptor -> descriptor.eventType().name())
                .thenComparing(descriptor -> nullToEmpty(descriptor.entityAlias()))
                .thenComparing(descriptor -> nullToEmpty(descriptor.actionCode()))
                .thenComparing(descriptor -> descriptor.target().getClass().getName())
                .thenComparing(descriptor -> descriptor.method().getName())
                .thenComparing(RuntimeEventHandlerDescriptor::beanName, Comparator.nullsLast(String::compareTo)));
        this.descriptors = List.copyOf(ordered);
    }

    public static RuntimeEventHandlerRegistry fromBeans(Map<String, Object> beans) {
        return fromBeans(beans, RuntimeEventHandlerRegistry::defaultInspectionType);
    }

    public static RuntimeEventHandlerRegistry fromBeans(Map<String, Object> beans,
                                                        Function<Object, Class<?>> inspectionTypeResolver) {
        return fromBeans(beans, inspectionTypeResolver, (bean, method) -> method);
    }

    public static RuntimeEventHandlerRegistry fromBeans(Map<String, Object> beans,
                                                        Function<Object, Class<?>> inspectionTypeResolver,
                                                        BiFunction<Object, Method, Method> invocableMethodResolver) {
        if (beans == null || beans.isEmpty()) {
            return new RuntimeEventHandlerRegistry(List.of());
        }
        Function<Object, Class<?>> resolver = inspectionTypeResolver == null
                ? RuntimeEventHandlerRegistry::defaultInspectionType
                : inspectionTypeResolver;
        BiFunction<Object, Method, Method> methodResolver = invocableMethodResolver == null
                ? (bean, method) -> method
                : invocableMethodResolver;
        List<RuntimeEventHandlerDescriptor> descriptors = new ArrayList<>();
        beans.forEach((beanName, bean) -> {
            Class<?> inspectionType = resolver.apply(bean);
            if (bean == null || inspectionType == null || moduleExtension(inspectionType) == null) {
                return;
            }
            for (Method method : handlerMethods(inspectionType)) {
                if (method.getAnnotation(RuntimeEventHandler.class) != null) {
                    descriptors.add(RuntimeEventHandlerDescriptor.from(
                            beanName,
                            bean,
                            method,
                            inspectionType,
                            methodResolver.apply(bean, method)
                    ));
                }
            }
        });
        return new RuntimeEventHandlerRegistry(descriptors);
    }

    public List<RuntimeEventHandlerDescriptor> descriptors() {
        return descriptors;
    }

    public List<RuntimeEventHandlerDescriptor> resolve(RuntimeEvent event) {
        return descriptors.stream()
                .filter(descriptor -> descriptor.supports(event))
                .toList();
    }

    public void dispatch(RuntimeEvent event) {
        for (RuntimeEventHandlerDescriptor descriptor : resolve(event)) {
            descriptor.handle(event);
        }
    }

    private static void validateUnique(List<RuntimeEventHandlerDescriptor> descriptors) {
        Set<String> uniqueKeys = new HashSet<>();
        for (RuntimeEventHandlerDescriptor descriptor : descriptors) {
            String key = String.join("|",
                    descriptor.moduleAlias(),
                    descriptor.eventType().name(),
                    nullToEmpty(descriptor.entityAlias()),
                    nullToEmpty(descriptor.actionCode()),
                    descriptor.target().getClass().getName(),
                    descriptor.method().getName());
            if (!uniqueKeys.add(key)) {
                throw new IllegalStateException("duplicate runtime event handler: " + key);
            }
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static Class<?> defaultInspectionType(Object bean) {
        return bean == null ? null : bean.getClass();
    }

    private static ModuleExtension moduleExtension(Class<?> type) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            ModuleExtension extension = current.getAnnotation(ModuleExtension.class);
            if (extension != null) {
                return extension;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static List<Method> handlerMethods(Class<?> type) {
        List<Method> methods = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getAnnotation(RuntimeEventHandler.class) != null) {
                    methods.add(method);
                }
            }
            current = current.getSuperclass();
        }
        return methods;
    }
}
