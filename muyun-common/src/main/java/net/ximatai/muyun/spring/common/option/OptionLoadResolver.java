package net.ximatai.muyun.spring.common.option;

import net.ximatai.muyun.spring.common.util.Preconditions;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Compiles static {@link OptionLoad} declarations against their option source fields. */
public final class OptionLoadResolver {
    private OptionLoadResolver() {
    }

    public static List<OptionLoadDefinition> resolve(Class<?> modelClass) {
        if (modelClass == null) {
            return List.of();
        }
        Map<String, OptionFieldDefinition> sources = OptionFieldResolver.resolve(modelClass).stream()
                .collect(Collectors.toUnmodifiableMap(OptionFieldDefinition::fieldName, definition -> definition));
        return OptionFieldResolver.fields(modelClass).stream()
                .map(field -> definition(field, sources))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private static OptionLoadDefinition definition(Field outputField,
                                                   Map<String, OptionFieldDefinition> sources) {
        OptionLoad annotation = outputField.getAnnotation(OptionLoad.class);
        if (annotation == null) {
            return null;
        }
        String sourceField = Preconditions.requireText(annotation.source(), "optionLoadSource");
        OptionFieldDefinition source = sources.get(sourceField);
        if (source == null) {
            throw new IllegalArgumentException("option load source is not an option field: "
                    + outputField.getDeclaringClass().getName() + "." + sourceField);
        }
        String itemField = Preconditions.requireText(annotation.field(), "optionLoadField");
        if (!optionItemFieldExists(itemField)) {
            throw new IllegalArgumentException("unknown option item field: " + itemField);
        }
        validateOutputType(outputField, itemField, source);
        return new OptionLoadDefinition(sourceField, outputField.getName(), itemField, source);
    }

    private static boolean optionItemFieldExists(String fieldName) {
        for (RecordComponent component : OptionItem.class.getRecordComponents()) {
            if (component.getName().equals(fieldName)) {
                return true;
            }
        }
        return false;
    }

    private static void validateOutputType(Field outputField,
                                           String itemField,
                                           OptionFieldDefinition source) {
        Class<?> itemValueType = optionItemFieldType(itemField);
        Class<?> outputType = wrap(outputField.getType());
        if (source.selectionMode() == OptionSelectionMode.MULTIPLE) {
            if (!outputType.isAssignableFrom(List.class)) {
                throw new IllegalArgumentException("multiple option load requires a List-compatible output field: "
                        + outputField.getDeclaringClass().getName() + "." + outputField.getName());
            }
            return;
        }
        if (!outputType.isAssignableFrom(itemValueType)) {
            throw new IllegalArgumentException("option load output field type does not accept " + itemField + ": "
                    + outputField.getDeclaringClass().getName() + "." + outputField.getName());
        }
    }

    private static Class<?> optionItemFieldType(String fieldName) {
        for (RecordComponent component : OptionItem.class.getRecordComponents()) {
            if (component.getName().equals(fieldName)) {
                return wrap(component.getType());
            }
        }
        throw new IllegalArgumentException("unknown option item field: " + fieldName);
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) return type;
        return switch (type.getName()) {
            case "boolean" -> Boolean.class;
            case "int" -> Integer.class;
            case "long" -> Long.class;
            case "double" -> Double.class;
            case "float" -> Float.class;
            case "short" -> Short.class;
            case "byte" -> Byte.class;
            case "char" -> Character.class;
            default -> type;
        };
    }
}
