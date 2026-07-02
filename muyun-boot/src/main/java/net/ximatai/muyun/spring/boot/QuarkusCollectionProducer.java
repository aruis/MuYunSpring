package net.ximatai.muyun.spring.boot;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class QuarkusCollectionProducer {
    @Produces
    @Dependent
    @SuppressWarnings("unchecked")
    <T> Optional<T> optional(InjectionPoint injectionPoint, BeanManager beanManager) {
        Class<?> itemType = itemType(injectionPoint.getType());
        Bean<?> bean = beanManager.resolve(beanManager.getBeans(itemType, Any.Literal.INSTANCE));
        if (bean == null) {
            return Optional.empty();
        }
        return Optional.of((T) itemType.cast(reference(beanManager, bean)));
    }

    @Produces
    @Dependent
    <T> List<T> list(InjectionPoint injectionPoint, BeanManager beanManager) {
        Class<?> itemType = itemType(injectionPoint.getType());
        return beanManager.getBeans(itemType, Any.Literal.INSTANCE).stream()
                .map(bean -> itemType.cast(reference(beanManager, bean)))
                .sorted(Comparator.comparing(item -> item.getClass().getName()))
                .map(item -> (T) item)
                .toList();
    }

    private Class<?> itemType(Type type) {
        if (!(type instanceof ParameterizedType parameterizedType)
                || parameterizedType.getActualTypeArguments().length == 0
                || !(parameterizedType.getActualTypeArguments()[0] instanceof Class<?> itemType)) {
            throw new IllegalStateException("collection injection point must declare item type: " + type);
        }
        return itemType;
    }

    private Object reference(BeanManager beanManager, Bean<?> bean) {
        CreationalContext<?> context = beanManager.createCreationalContext(bean);
        return beanManager.getReference(bean, bean.getBeanClass(), context);
    }
}
