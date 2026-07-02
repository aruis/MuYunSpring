package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.ability.event.ModuleExtension;
import net.ximatai.muyun.spring.ability.event.RuntimeEventHandlerListener;
import net.ximatai.muyun.spring.ability.event.RuntimeEventHandlerRegistry;
import net.ximatai.muyun.spring.ability.event.RuntimeEventListener;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
public class MuYunSpringRuntimeEventHandlerConfiguration {
    @Produces
    @ApplicationScoped
    @DefaultBean
    RuntimeEventHandlerRegistry runtimeEventHandlerRegistry(BeanManager beanManager) {
        Map<String, Object> beans = new LinkedHashMap<>();
        for (Bean<?> bean : beanManager.getBeans(Object.class, Any.Literal.INSTANCE)) {
            if (findAnnotation(bean.getBeanClass(), ModuleExtension.class) == null) {
                continue;
            }
            beans.put(bean.getBeanClass().getName(), reference(beanManager, bean));
        }
        return RuntimeEventHandlerRegistry.fromBeans(beans);
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    RuntimeEventListener runtimeEventHandlerListener(RuntimeEventHandlerRegistry registry) {
        return new RuntimeEventHandlerListener(registry);
    }

    private static Object reference(BeanManager beanManager, Bean<?> bean) {
        CreationalContext<?> context = beanManager.createCreationalContext(bean);
        return beanManager.getReference(bean, bean.getBeanClass(), context);
    }

    private static <A extends Annotation> A findAnnotation(Class<?> type, Class<A> annotationType) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            A annotation = current.getAnnotation(annotationType);
            if (annotation != null) {
                return annotation;
            }
            current = current.getSuperclass();
        }
        return null;
    }
}
