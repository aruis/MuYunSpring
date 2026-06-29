package net.ximatai.muyun.spring.common.option;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
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
        validateSelectionMode(field, annotation);
        OptionBinding binding = resolveBinding(field, annotation);
        return Optional.of(new OptionFieldDefinition(
                field.getName(),
                binding,
                annotation.selectionMode(),
                annotation.titleOutput(),
                resolveTitleOutputField(field.getDeclaringClass(), field.getName(), annotation)));
    }

    private static OptionBinding resolveBinding(Field field, OptionField annotation) {
        if (annotation.type() == OptionSourceType.DICTIONARY) {
            rejectEnumType(annotation);
            OptionBinding binding = new OptionBinding(OptionBinding.DICTIONARY_SOURCE, annotation.source());
            binding.dictionarySource();
            return binding;
        }
        if (annotation.type() == OptionSourceType.ENUM) {
            rejectEnumSource(annotation);
            return enumBinding(resolveEnumType(field, annotation));
        }
        throw new IllegalArgumentException("unsupported option source type: " + annotation.type());
    }

    private static void validateSelectionMode(Field field, OptionField annotation) {
        boolean multipleValueType = isMultipleValueType(field);
        if (multipleValueType && annotation.selectionMode() != OptionSelectionMode.MULTIPLE) {
            throw new IllegalArgumentException("multiple option field requires MULTIPLE selection mode: "
                    + qualifiedFieldName(field));
        }
        if (!multipleValueType && annotation.selectionMode() == OptionSelectionMode.MULTIPLE) {
            throw new IllegalArgumentException("MULTIPLE selection mode requires collection or array field: "
                    + qualifiedFieldName(field));
        }
    }

    private static void rejectEnumType(OptionField annotation) {
        if (annotation.enumType() != CodeTitleEnum.class) {
            throw new IllegalArgumentException("dictionary option field must not declare enumType");
        }
    }

    private static void rejectEnumSource(OptionField annotation) {
        if (!annotation.source().isBlank()) {
            throw new IllegalArgumentException("enum option field must use enumType or field type, not source");
        }
    }

    private static Class<?> resolveEnumType(Field field, OptionField annotation) {
        if (annotation.enumType() != CodeTitleEnum.class) {
            return requireCodeTitleEnum(annotation.enumType(), "enumType");
        }
        Class<?> fieldType = field.getType();
        if (isCodeTitleEnum(fieldType)) {
            return requireCodeTitleEnum(fieldType, "option field type");
        }
        if (Collection.class.isAssignableFrom(fieldType)) {
            Class<?> elementType = collectionElementType(field);
            if (isCodeTitleEnum(elementType)) {
                return requireCodeTitleEnum(elementType, "option collection element type");
            }
        }
        if (fieldType.isArray()) {
            Class<?> elementType = arrayElementType(field);
            if (isCodeTitleEnum(elementType)) {
                return requireCodeTitleEnum(elementType, "option array element type");
            }
        }
        throw new IllegalArgumentException("enum option field requires enumType or CodeTitleEnum field type: "
                + qualifiedFieldName(field));
    }

    private static boolean isCodeTitleEnum(Class<?> type) {
        return type != null && type.isEnum() && CodeTitleEnum.class.isAssignableFrom(type);
    }

    private static boolean isMultipleValueType(Field field) {
        return Collection.class.isAssignableFrom(field.getType()) || field.getType().isArray();
    }

    private static Class<?> collectionElementType(Field field) {
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType parameterizedType)) {
            return null;
        }
        Type[] arguments = parameterizedType.getActualTypeArguments();
        if (arguments.length != 1 || !(arguments[0] instanceof Class<?> elementType)) {
            return null;
        }
        return elementType;
    }

    private static Class<?> arrayElementType(Field field) {
        return field.getType().getComponentType();
    }

    private static Class<?> requireCodeTitleEnum(Class<?> type, String label) {
        if (!isCodeTitleEnum(type)) {
            throw new IllegalArgumentException(label + " must be CodeTitleEnum enum: " + type.getName());
        }
        return type;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static OptionBinding enumBinding(Class<?> enumType) {
        return OptionBinding.enumType((Class) enumType);
    }

    private static String qualifiedFieldName(Field field) {
        return field.getDeclaringClass().getName() + "." + field.getName();
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
