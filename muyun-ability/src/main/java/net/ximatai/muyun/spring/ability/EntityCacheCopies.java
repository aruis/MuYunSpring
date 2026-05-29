package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.ability.child.ChildRef;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

final class EntityCacheCopies {
    private EntityCacheCopies() {
    }

    static <T> T shallowCopy(T entity) {
        if (entity == null) {
            return null;
        }
        T copy = newInstance(entity);
        Class<?> current = entity.getClass();
        while (current != null && !Object.class.equals(current)) {
            copyFields(entity, copy, current);
            current = current.getSuperclass();
        }
        return copy;
    }

    private static <T> T newInstance(T entity) {
        try {
            @SuppressWarnings("unchecked")
            Constructor<T> constructor = (Constructor<T>) entity.getClass().getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new PlatformException("cache copy requires a no-arg constructor or custom copyForCache: "
                    + entity.getClass().getName(), e);
        }
    }

    private static void copyFields(Object source, Object target, Class<?> owner) {
        for (Field field : owner.getDeclaredFields()) {
            if (shouldSkip(field)) {
                continue;
            }
            try {
                field.setAccessible(true);
                field.set(target, field.get(source));
            } catch (IllegalAccessException e) {
                throw new PlatformException("cannot copy cache field: "
                        + owner.getName() + "." + field.getName(), e);
            }
        }
    }

    private static boolean shouldSkip(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers)
                || Modifier.isTransient(modifiers)
                || field.getAnnotation(ChildRef.class) != null;
    }
}
