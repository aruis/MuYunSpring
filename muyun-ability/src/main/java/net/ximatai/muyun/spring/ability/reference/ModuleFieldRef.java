package net.ximatai.muyun.spring.ability.reference;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

public record ModuleFieldRef(Class<?> ownerType, String fieldName) {
    public ModuleFieldRef {
        if (ownerType == null) {
            throw new IllegalArgumentException("module field owner type must not be null");
        }
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("module field name must not be blank");
        }
        fieldName = fieldName.trim();
    }

    public static ModuleFieldRef of(ModuleProperty<?, ?> property) {
        if (property == null) {
            throw new IllegalArgumentException("module property must not be null");
        }
        SerializedLambda lambda = serializedLambda(property);
        return new ModuleFieldRef(ownerType(lambda), fieldName(lambda.getImplMethodName()));
    }

    private static SerializedLambda serializedLambda(Object property) {
        try {
            Method writeReplace = property.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            Object serialized = writeReplace.invoke(property);
            if (serialized instanceof SerializedLambda lambda) {
                return lambda;
            }
            throw new IllegalArgumentException("module property is not a serialized lambda");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("module property must be a method reference", exception);
        }
    }

    private static Class<?> ownerType(SerializedLambda lambda) {
        String className = lambda.getImplClass().replace('/', '.');
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            throw new IllegalArgumentException("module property owner class not found: " + className, exception);
        }
    }

    private static String fieldName(String methodName) {
        if (methodName == null || methodName.isBlank()) {
            throw new IllegalArgumentException("module property method name must not be blank");
        }
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return decapitalize(methodName.substring(3));
        }
        if (methodName.startsWith("is") && methodName.length() > 2) {
            return decapitalize(methodName.substring(2));
        }
        throw new IllegalArgumentException("module property must reference a getter method: " + methodName);
    }

    private static String decapitalize(String value) {
        if (value.length() > 1 && Character.isUpperCase(value.charAt(0)) && Character.isUpperCase(value.charAt(1))) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }
}
