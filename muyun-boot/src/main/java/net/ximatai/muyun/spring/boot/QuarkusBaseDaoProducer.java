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
import java.util.Set;

@ApplicationScoped
public class QuarkusBaseDaoProducer {
    @Produces
    @Dependent
    @SuppressWarnings("unchecked")
    <T extends EntityContract> BaseDao<T, String> baseDao(InjectionPoint injectionPoint, BeanManager beanManager) {
        Class<?> modelClass = modelClass(injectionPoint.getType());
        Set<Bean<?>> beans = beanManager.getBeans(BaseDao.class, Any.Literal.INSTANCE);
        for (Bean<?> bean : beans) {
            if (bean == injectionPoint.getBean()) {
                continue;
            }
            if (supportsModel(bean, modelClass)) {
                CreationalContext<?> context = beanManager.createCreationalContext(bean);
                return (BaseDao<T, String>) beanManager.getReference(bean, BaseDao.class, context);
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

    private boolean supportsModel(Bean<?> bean, Class<?> modelClass) {
        for (Type type : bean.getTypes()) {
            if (type instanceof ParameterizedType parameterizedType
                    && parameterizedType.getRawType() == BaseDao.class
                    && parameterizedType.getActualTypeArguments().length > 0
                    && parameterizedType.getActualTypeArguments()[0] == modelClass) {
                return true;
            }
        }
        return false;
    }
}
