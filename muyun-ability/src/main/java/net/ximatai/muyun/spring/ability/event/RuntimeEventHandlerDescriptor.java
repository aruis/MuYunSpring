package net.ximatai.muyun.spring.ability.event;

import net.ximatai.muyun.spring.ability.TransactionScopeSupport;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public record RuntimeEventHandlerDescriptor(
        String beanName,
        Object target,
        Method method,
        String moduleAlias,
        RuntimeEventType eventType,
        String entityAlias,
        String actionCode,
        int order,
        RuntimeEventHandlerPhase phase,
        RuntimeEventHandlerFailurePolicy failure
) {
    private static final Logger logger = Logger.getLogger(RuntimeEventHandlerDescriptor.class.getName());

    public RuntimeEventHandlerDescriptor {
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(method, "method must not be null");
        requireText(moduleAlias, "moduleAlias");
        Objects.requireNonNull(eventType, "eventType must not be null");
        entityAlias = normalize(entityAlias);
        actionCode = normalize(actionCode);
        phase = effectivePhase(phase, eventType);
        failure = effectiveFailure(failure, eventType);
        rejectAfterCommitBlockingHandler(phase, failure, target, method);
        method.setAccessible(true);
    }

    public static RuntimeEventHandlerDescriptor from(String beanName, Object target, Method method) {
        return from(beanName, target, method, target == null ? null : target.getClass(), method);
    }

    public static RuntimeEventHandlerDescriptor from(String beanName,
                                                     Object target,
                                                     Method sourceMethod,
                                                     Class<?> extensionType,
                                                     Method invocableMethod) {
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(sourceMethod, "sourceMethod must not be null");
        Objects.requireNonNull(invocableMethod, "invocableMethod must not be null");
        RuntimeEventHandler handler = sourceMethod.getAnnotation(RuntimeEventHandler.class);
        if (handler == null) {
            throw new IllegalArgumentException("method is not a RuntimeEventHandler: " + sourceMethod);
        }
        ModuleExtension extension = moduleExtension(extensionType);
        if (extension == null) {
            throw new IllegalArgumentException("runtime event handler target is not a ModuleExtension: "
                    + target.getClass().getName());
        }
        validateSignature(sourceMethod);
        validateSignature(invocableMethod);
        return new RuntimeEventHandlerDescriptor(
                beanName,
                target,
                invocableMethod,
                moduleAlias(extension),
                handler.event(),
                entityAlias(extension, handler),
                handler.actionCode(),
                handler.order(),
                handler.phase(),
                handler.failure()
        );
    }

    public boolean supports(RuntimeEvent event) {
        if (event == null || event.eventType() != eventType) {
            return false;
        }
        if (!Objects.equals(moduleAlias, normalize(event.moduleAlias()))) {
            return false;
        }
        if (entityAlias != null && !Objects.equals(entityAlias, normalize(event.entityAlias()))) {
            return false;
        }
        return actionCode == null || Objects.equals(actionCode, normalize(event.actionCode()));
    }

    public void handle(RuntimeEvent event) {
        Runnable action = () -> invoke(event);
        if (phase == RuntimeEventHandlerPhase.AFTER_COMMIT) {
            TransactionScopeSupport.afterCommitOrNow(action);
        } else {
            action.run();
        }
    }

    private void invoke(RuntimeEvent event) {
        try {
            method.invoke(target, event);
        } catch (InvocationTargetException ex) {
            handleFailure(event, ex.getTargetException());
        } catch (ReflectiveOperationException | RuntimeException ex) {
            handleFailure(event, ex);
        }
    }

    private void handleFailure(RuntimeEvent event, Throwable ex) {
        String message = "runtime event handler failed, module=%s, event=%s, handler=%s#%s"
                .formatted(event.moduleAlias(), event.eventType(), target.getClass().getName(), method.getName());
        if (failure == RuntimeEventHandlerFailurePolicy.WARN) {
            logger.log(Level.WARNING, message, ex);
            return;
        }
        if (ex instanceof RuntimeEventHandlerException handlerException) {
            throw handlerException;
        }
        throw new RuntimeEventHandlerException(message, ex);
    }

    private static void validateSignature(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 1 && parameterTypes[0] == RuntimeEvent.class) {
            return;
        }
        throw new IllegalStateException("runtime event handler method signature must be (RuntimeEvent): "
                + method.getDeclaringClass().getName() + "#" + method.getName());
    }

    private static String moduleAlias(ModuleExtension extension) {
        String moduleAlias = normalize(extension.moduleAlias());
        String value = normalize(extension.value());
        if (moduleAlias != null && value != null && !moduleAlias.equals(value)) {
            throw new IllegalStateException("@ModuleExtension value and moduleAlias conflict: "
                    + moduleAlias + " != " + value);
        }
        return moduleAlias == null ? value : moduleAlias;
    }

    private static String entityAlias(ModuleExtension extension, RuntimeEventHandler handler) {
        String methodEntityAlias = normalize(handler.entityAlias());
        return methodEntityAlias == null ? normalize(extension.entityAlias()) : methodEntityAlias;
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

    private static RuntimeEventHandlerPhase effectivePhase(RuntimeEventHandlerPhase phase, RuntimeEventType eventType) {
        if (phase != null && phase != RuntimeEventHandlerPhase.DEFAULT) {
            return phase;
        }
        return isAfterEvent(eventType) ? RuntimeEventHandlerPhase.AFTER_COMMIT : RuntimeEventHandlerPhase.IN_TRANSACTION;
    }

    private static RuntimeEventHandlerFailurePolicy effectiveFailure(RuntimeEventHandlerFailurePolicy failure,
                                                                     RuntimeEventType eventType) {
        if (failure != null && failure != RuntimeEventHandlerFailurePolicy.DEFAULT) {
            return failure;
        }
        return isAfterEvent(eventType) ? RuntimeEventHandlerFailurePolicy.WARN : RuntimeEventHandlerFailurePolicy.BLOCK;
    }

    private static void rejectAfterCommitBlockingHandler(RuntimeEventHandlerPhase phase,
                                                         RuntimeEventHandlerFailurePolicy failure,
                                                         Object target,
                                                         Method method) {
        if (phase == RuntimeEventHandlerPhase.AFTER_COMMIT
                && failure == RuntimeEventHandlerFailurePolicy.BLOCK) {
            throw new IllegalStateException("AFTER_COMMIT runtime event handler cannot use BLOCK failure policy: "
                    + target.getClass().getName() + "#" + method.getName());
        }
    }

    private static boolean isAfterEvent(RuntimeEventType eventType) {
        return eventType.name().startsWith("AFTER_")
                || eventType == RuntimeEventType.ACTION_EXECUTED
                || eventType == RuntimeEventType.MODULE_PUBLISHED;
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
