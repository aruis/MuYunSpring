package net.ximatai.muyun.spring.common.security;

public enum FieldMaskingPolicy {
    NONE,
    FULL,
    MIDDLE,
    PHONE,
    EMAIL;

    public boolean enabled() {
        return this != NONE;
    }
}
