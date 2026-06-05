package net.ximatai.muyun.spring.ability.security;

import net.ximatai.muyun.spring.common.security.FieldProtectionException;

public interface FieldSigner {
    FieldSigner UNAVAILABLE = (fieldName, plainValue) -> {
        throw new FieldProtectionException("field signer is not configured: " + fieldName);
    };

    String sign(String fieldName, Object plainValue);

    default void verify(String fieldName, Object plainValue, String signature) {
        if (signature == null || signature.isBlank()) {
            throw new FieldProtectionException("field signature is missing: " + fieldName);
        }
        String expected = sign(fieldName, plainValue);
        if (!signature.equals(expected)) {
            throw new FieldProtectionException("field signature mismatch: " + fieldName);
        }
    }
}
