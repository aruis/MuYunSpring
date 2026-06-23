package net.ximatai.muyun.spring.common.exception;

public class AuthenticationRequiredException extends PlatformException {
    public AuthenticationRequiredException(String message) {
        super(PlatformErrorCodes.AUTH_REQUIRED, 401, message);
    }

    public AuthenticationRequiredException(String message, Throwable cause) {
        super(PlatformErrorCodes.AUTH_REQUIRED, 401, message, cause);
    }
}
