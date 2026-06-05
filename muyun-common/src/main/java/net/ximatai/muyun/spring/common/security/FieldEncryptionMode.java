package net.ximatai.muyun.spring.common.security;

public enum FieldEncryptionMode {
    NONE,
    ENCRYPTED;

    public boolean enabled() {
        return this != NONE;
    }
}
