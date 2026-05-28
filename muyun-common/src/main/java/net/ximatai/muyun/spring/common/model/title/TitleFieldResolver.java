package net.ximatai.muyun.spring.common.model.title;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Resolves the field marked as a record's display title.
 */
public final class TitleFieldResolver {
    private static final ConcurrentMap<Class<?>, Optional<Field>> TITLE_FIELDS = new ConcurrentHashMap<>();

    private TitleFieldResolver() {
    }

    public static Optional<String> resolveFieldName(Class<?> modelClass) {
        return resolve(modelClass).map(Field::getName);
    }

    public static Object read(Object record) {
        if (record == null) {
            return null;
        }
        return resolve(record.getClass())
                .map(field -> readField(field, record))
                .orElse(null);
    }

    public static String readAsString(Object record) {
        Object value = read(record);
        return value == null ? null : String.valueOf(value);
    }

    static void clearCacheForTests() {
        TITLE_FIELDS.clear();
    }

    private static Optional<Field> resolve(Class<?> modelClass) {
        if (modelClass == null) {
            return Optional.empty();
        }
        return TITLE_FIELDS.computeIfAbsent(modelClass, TitleFieldResolver::loadTitleField);
    }

    private static Optional<Field> loadTitleField(Class<?> modelClass) {
        Class<?> current = modelClass;
        List<Field> titleFields = new ArrayList<>();
        while (current != null && !Object.class.equals(current)) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getAnnotation(TitleField.class) == null) {
                    continue;
                }
                titleFields.add(accessible(field, modelClass));
            }
            current = current.getSuperclass();
        }
        if (titleFields.size() > 1) {
            throw new IllegalStateException("multiple title fields in class hierarchy: " + modelClass.getName());
        }
        if (titleFields.size() == 1) {
            return Optional.of(titleFields.getFirst());
        }
        return Optional.empty();
    }

    private static Field accessible(Field field, Class<?> modelClass) {
        try {
            field.setAccessible(true);
            return field;
        } catch (RuntimeException e) {
            throw new IllegalStateException("cannot access title field: "
                    + modelClass.getName() + "." + field.getName(), e);
        }
    }

    private static Object readField(Field field, Object record) {
        try {
            return field.get(record);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("cannot read title field: "
                    + record.getClass().getName() + "." + field.getName(), e);
        }
    }
}
