package net.ximatai.muyun.spring.boot;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class QuarkusBaseDaoProducer {
    @Produces
    @Dependent
    @SuppressWarnings("unchecked")
    <T extends EntityContract> BaseDao<T, String> baseDao(InjectionPoint injectionPoint, BeanManager beanManager) {
        Class<?> modelClass = modelClass(injectionPoint.getType());
        Set<Bean<?>> beans = new LinkedHashSet<>(beanManager.getBeans(BaseDao.class, Any.Literal.INSTANCE));
        beans.addAll(beanManager.getBeans(Object.class, Any.Literal.INSTANCE));
        for (Bean<?> bean : beans) {
            if (bean == injectionPoint.getBean()) {
                continue;
            }
            Optional<Type> daoType = daoType(bean, modelClass);
            if (daoType.isPresent()) {
                CreationalContext<?> context = beanManager.createCreationalContext(bean);
                return (BaseDao<T, String>) beanManager.getReference(bean, daoType.get(), context);
            }
        }
        throw new IllegalStateException("BaseDao bean is not found for model: " + modelClass.getName());
    }

    private Class<?> modelClass(Type type) {
        if (!(type instanceof ParameterizedType parameterizedType)
                || parameterizedType.getActualTypeArguments().length == 0
                || !(parameterizedType.getActualTypeArguments()[0] instanceof Class<?> modelClass)) {
            throw new IllegalStateException("BaseDao injection point must declare model type: " + type);
        }
        return modelClass;
    }

    private Optional<Type> daoType(Bean<?> bean, Class<?> modelClass) {
        for (Type type : bean.getTypes()) {
            if (supportsModel(type, modelClass)) {
                return Optional.of(type);
            }
        }
        return daoType(bean.getBeanClass(), modelClass);
    }

    private Optional<Type> daoType(Type type, Class<?> modelClass) {
        if (supportsModel(type, modelClass)) {
            return Optional.of(type);
        }
        if (type instanceof ParameterizedType parameterizedType) {
            return daoType(parameterizedType.getRawType(), modelClass);
        }
        if (!(type instanceof Class<?> beanClass) || beanClass == Object.class) {
            return Optional.empty();
        }
        for (Type interfaceType : beanClass.getGenericInterfaces()) {
            Optional<Type> matched = daoType(interfaceType, modelClass);
            if (matched.isPresent()) {
                return matched;
            }
        }
        return daoType(beanClass.getGenericSuperclass(), modelClass);
    }

    private boolean supportsModel(Type type, Class<?> modelClass) {
        if (type instanceof ParameterizedType parameterizedType) {
            if (parameterizedType.getRawType() == BaseDao.class
                    && parameterizedType.getActualTypeArguments().length > 0
                    && parameterizedType.getActualTypeArguments()[0] == modelClass) {
                return true;
            }
            return supportsModel(parameterizedType.getRawType(), modelClass);
        }
        if (!(type instanceof Class<?> beanClass) || beanClass == Object.class) {
            return false;
        }
        for (Type interfaceType : beanClass.getGenericInterfaces()) {
            if (supportsModel(interfaceType, modelClass)) {
                return true;
            }
        }
        return supportsModel(beanClass.getGenericSuperclass(), modelClass);
    }
}
