package net.ximatai.muyun.spring.ability.security;

import net.ximatai.muyun.spring.common.security.EncryptedField;
import net.ximatai.muyun.spring.common.security.FieldEncryptionMode;
import net.ximatai.muyun.spring.common.security.FieldMaskingPolicy;
import net.ximatai.muyun.spring.common.security.FieldProtectionDefinition;
import net.ximatai.muyun.spring.common.security.FieldProtectionException;
import net.ximatai.muyun.spring.common.security.FieldSignatureMode;
import net.ximatai.muyun.spring.common.security.MaskedField;
import net.ximatai.muyun.spring.common.security.SignedField;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class StaticFieldProtectionResolver {
    private StaticFieldProtectionResolver() {
    }

    public static <T> FieldProtectionPlan<T> resolve(Class<?> modelClass) {
        if (modelClass == null) {
            return FieldProtectionPlan.empty();
        }
        List<ProtectedFieldAccessor<T>> fields = new ArrayList<>();
        for (Field field : declaredFields(modelClass)) {
            FieldProtectionDefinition protection = protection(field);
            if (!protection.enabled()) {
                continue;
            }
            Field signatureField = protection.signatureMode().enabled()
                    ? findField(modelClass, field.getName() + "Signature")
                    : null;
            if (protection.signatureMode().enabled() && signatureField == null) {
                throw new FieldProtectionException("Signature companion field is missing: "
                        + modelClass.getName() + "." + field.getName() + "Signature");
            }
            fields.add(new ReflectionProtectedFieldAccessor<>(field, signatureField, protection));
        }
        return new FieldProtectionPlan<>(fields);
    }

    private static FieldProtectionDefinition protection(Field field) {
        FieldEncryptionMode encryptionMode = field.isAnnotationPresent(EncryptedField.class)
                ? FieldEncryptionMode.ENCRYPTED
                : FieldEncryptionMode.NONE;
        FieldSignatureMode signatureMode = field.isAnnotationPresent(SignedField.class)
                ? FieldSignatureMode.SIGNED
                : FieldSignatureMode.NONE;
        MaskedField masked = field.getAnnotation(MaskedField.class);
        FieldMaskingPolicy maskingPolicy = masked == null ? FieldMaskingPolicy.NONE : masked.value();
        return new FieldProtectionDefinition(encryptionMode, signatureMode, maskingPolicy);
    }

    private static List<Field> declaredFields(Class<?> modelClass) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = modelClass;
        while (current != null && current != Object.class) {
            fields.addAll(List.of(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    private static Field findField(Class<?> modelClass, String fieldName) {
        Class<?> current = modelClass;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
