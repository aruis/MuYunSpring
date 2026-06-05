package net.ximatai.muyun.spring.ability.security;

import net.ximatai.muyun.spring.common.security.FieldProtectionDefinition;
import net.ximatai.muyun.spring.common.security.FieldProtectionException;

import java.lang.reflect.Field;

final class ReflectionProtectedFieldAccessor<T> implements ProtectedFieldAccessor<T> {
    private final Field field;
    private final Field signatureField;
    private final FieldProtectionDefinition protection;

    ReflectionProtectedFieldAccessor(Field field, Field signatureField, FieldProtectionDefinition protection) {
        this.field = field;
        this.field.setAccessible(true);
        this.signatureField = signatureField;
        if (this.signatureField != null) {
            this.signatureField.setAccessible(true);
        }
        this.protection = protection;
    }

    @Override
    public String fieldName() {
        return field.getName();
    }

    @Override
    public FieldProtectionDefinition protection() {
        return protection;
    }

    @Override
    public Object get(T entity) {
        try {
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new FieldProtectionException("Cannot read protected field: " + field.getName(), e);
        }
    }

    @Override
    public void set(T entity, Object value) {
        try {
            field.set(entity, value);
        } catch (IllegalAccessException e) {
            throw new FieldProtectionException("Cannot write protected field: " + field.getName(), e);
        }
    }

    @Override
    public Object getSignature(T entity) {
        if (signatureField == null) {
            return null;
        }
        try {
            return signatureField.get(entity);
        } catch (IllegalAccessException e) {
            throw new FieldProtectionException("Cannot read signature field: " + signatureField.getName(), e);
        }
    }

    @Override
    public void setSignature(T entity, String signature) {
        if (signatureField == null) {
            throw new FieldProtectionException("Signature companion field is missing: " + signatureFieldName());
        }
        try {
            signatureField.set(entity, signature);
        } catch (IllegalAccessException e) {
            throw new FieldProtectionException("Cannot write signature field: " + signatureField.getName(), e);
        }
    }
}
