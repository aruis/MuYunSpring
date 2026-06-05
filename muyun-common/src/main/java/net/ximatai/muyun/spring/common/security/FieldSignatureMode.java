package net.ximatai.muyun.spring.common.security;

public enum FieldSignatureMode {
    NONE,
    SIGNED;

    public boolean enabled() {
        return this != NONE;
    }
}
