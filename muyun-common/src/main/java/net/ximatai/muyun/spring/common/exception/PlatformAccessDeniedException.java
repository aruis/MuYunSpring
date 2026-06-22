package net.ximatai.muyun.spring.common.exception;

public class PlatformAccessDeniedException extends PlatformException {
    public PlatformAccessDeniedException(String message) {
        super(PlatformErrorCodes.ACCESS_DENIED, 403, message);
    }

    public PlatformAccessDeniedException(String message, Throwable cause) {
        super(PlatformErrorCodes.ACCESS_DENIED, 403, message, cause);
    }
}
