package net.ximatai.muyun.spring.common.exception;

public class PlatformAccessDeniedException extends PlatformException {
    public PlatformAccessDeniedException(String message) {
        super(message);
    }

    public PlatformAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
