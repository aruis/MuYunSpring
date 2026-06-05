package net.ximatai.muyun.spring.common.security;

public class FieldProtectionException extends RuntimeException {
    public FieldProtectionException(String message) {
        super(message);
    }

    public FieldProtectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
