package net.ximatai.muyun.spring.common.exception;

public class AuthenticationRequiredException extends PlatformException {
    public AuthenticationRequiredException(String message) {
        super(message);
    }

    public AuthenticationRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
