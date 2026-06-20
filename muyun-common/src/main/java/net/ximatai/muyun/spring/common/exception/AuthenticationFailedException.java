package net.ximatai.muyun.spring.common.exception;

public class AuthenticationFailedException extends PlatformException {
    public AuthenticationFailedException(String message) {
        super(message);
    }

    public AuthenticationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
