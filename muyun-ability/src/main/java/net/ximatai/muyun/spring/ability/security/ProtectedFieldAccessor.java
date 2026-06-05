package net.ximatai.muyun.spring.ability.security;

import net.ximatai.muyun.spring.common.security.FieldProtectionDefinition;

public interface ProtectedFieldAccessor<T> {
    String fieldName();

    FieldProtectionDefinition protection();

    Object get(T entity);

    void set(T entity, Object value);

    default String signatureFieldName() {
        return fieldName() + "Signature";
    }

    Object getSignature(T entity);

    void setSignature(T entity, String signature);

    default boolean hasValue(T entity) {
        return get(entity) != null;
    }

    default boolean shouldWrite(T entity) {
        return true;
    }
}
