package net.ximatai.muyun.spring.ability.security;

import net.ximatai.muyun.spring.common.security.FieldProtectionException;

public interface FieldCryptoProvider {
    FieldCryptoProvider UNAVAILABLE = new FieldCryptoProvider() {
        @Override
        public String encrypt(String fieldName, Object plainValue) {
            throw new FieldProtectionException("field crypto provider is not configured: " + fieldName);
        }

        @Override
        public Object decrypt(String fieldName, String protectedValue) {
            throw new FieldProtectionException("field crypto provider is not configured: " + fieldName);
        }
    };

    String encrypt(String fieldName, Object plainValue);

    Object decrypt(String fieldName, String protectedValue);
}
