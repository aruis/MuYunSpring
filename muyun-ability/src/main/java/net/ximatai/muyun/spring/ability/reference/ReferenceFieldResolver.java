package net.ximatai.muyun.spring.ability.reference;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import java.lang.reflect.Field;

final class ReferenceFieldResolver {
    private ReferenceFieldResolver() {
    }

    static Object read(Object record, String fieldName) {
        if (record == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }
        Class<?> current = record.getClass();
        while (current != null && !Object.class.equals(current)) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(record);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new PlatformException("Cannot read reference projection field: "
                        + record.getClass().getName() + "." + fieldName, e);
            }
        }
        throw new PlatformException("Cannot find reference projection field: "
                + record.getClass().getName() + "." + fieldName);
    }
}
