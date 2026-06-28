package net.ximatai.muyun.spring.common.option;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class OptionFieldResolver {
    private OptionFieldResolver() {
    }

    public static List<OptionFieldDefinition> resolve(Class<?> modelClass) {
        if (modelClass == null) {
            return List.of();
        }
        List<OptionFieldDefinition> definitions = new ArrayList<>();
        Class<?> current = modelClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                resolve(field).ifPresent(definitions::add);
            }
            current = current.getSuperclass();
        }
        return List.copyOf(definitions);
    }

    public static Optional<OptionFieldDefinition> resolve(Field field) {
        if (field == null) {
            return Optional.empty();
        }
        OptionField annotation = field.getAnnotation(OptionField.class);
        if (annotation == null) {
            return Optional.empty();
        }
        return Optional.of(new OptionFieldDefinition(
                field.getName(),
                annotation.type().toBinding(annotation.source()),
                annotation.selectionMode(),
                annotation.titleOutput(),
                resolveTitleOutputField(field.getDeclaringClass(), field.getName(), annotation)));
    }

    private static String resolveTitleOutputField(Class<?> declaringClass, String fieldName, OptionField annotation) {
        if (annotation.titleOutput() == OptionTitleOutput.NONE) {
            return "";
        }
        String outputField = annotation.titleOutput() == OptionTitleOutput.CUSTOM
                ? annotation.titleOutputField()
                : autoTitleOutputField(fieldName);
        requireField(declaringClass, outputField, "option title output field");
        return outputField;
    }

    private static String autoTitleOutputField(String fieldName) {
        return fieldName + "Title";
    }

    private static void requireField(Class<?> declaringClass, String fieldName, String label) {
        Class<?> current = declaringClass;
        while (current != null && current != Object.class) {
            try {
                current.getDeclaredField(fieldName);
                return;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new IllegalArgumentException(label + " does not exist: "
                + declaringClass.getName() + "." + fieldName);
    }
}
