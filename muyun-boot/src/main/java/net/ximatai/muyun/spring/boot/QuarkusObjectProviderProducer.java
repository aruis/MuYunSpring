package net.ximatai.muyun.spring.boot;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import net.ximatai.muyun.spring.common.di.ObjectProvider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Dependent
public class QuarkusObjectProviderProducer {
    private final BeanManager beanManager;

    public QuarkusObjectProviderProducer(BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    @Produces
    @Dependent
    public <T> ObjectProvider<T> objectProvider(InjectionPoint injectionPoint) {
        Type type = injectionPoint.getType();
        if (!(type instanceof ParameterizedType parameterizedType)
                || parameterizedType.getActualTypeArguments().length != 1) {
            throw new IllegalStateException("ObjectProvider injection point must declare a target type: "
                    + injectionPoint);
        }
        Type targetType = parameterizedType.getActualTypeArguments()[0];
        return new CdiObjectProvider<>(beanManager, targetType);
    }

    private static final class CdiObjectProvider<T> implements ObjectProvider<T> {
        private final BeanManager beanManager;
        private final Type targetType;

        private CdiObjectProvider(BeanManager beanManager, Type targetType) {
            this.beanManager = beanManager;
            this.targetType = targetType;
        }

        @Override
        public T getIfAvailable() {
            Set<Bean<?>> beans = beanManager.getBeans(targetType, Any.Literal.INSTANCE);
            if (beans == null || beans.isEmpty()) {
                return null;
            }
            Bean<?> bean = beanManager.resolve(beans);
            if (bean == null) {
                return null;
            }
            return reference(bean);
        }

        @Override
        public Stream<T> orderedStream() {
            Set<Bean<?>> beans = beanManager.getBeans(targetType, Any.Literal.INSTANCE);
            if (beans == null || beans.isEmpty()) {
                return Stream.empty();
            }
            List<Bean<?>> ordered = beans.stream()
                    .sorted(Comparator.comparingInt(QuarkusObjectProviderProducer::priority)
                            .thenComparing(bean -> bean.getBeanClass().getName()))
                    .toList();
            return ordered.stream().map(this::reference);
        }

        @SuppressWarnings("unchecked")
        private T reference(Bean<?> bean) {
            CreationalContext<?> context = beanManager.createCreationalContext(bean);
            return (T) beanManager.getReference(bean, targetType, context);
        }
    }

    private static int priority(Bean<?> bean) {
        Priority priority = bean.getBeanClass().getAnnotation(Priority.class);
        return priority == null ? 0 : priority.value();
    }
}
