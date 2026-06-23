package net.ximatai.muyun.spring.common.exception;

public class AuthenticationFailedException extends PlatformException {
    public AuthenticationFailedException(String message) {
        super(PlatformErrorCodes.LOGIN_BAD_CREDENTIALS, 401, message);
    }

    public AuthenticationFailedException(String message, Throwable cause) {
        super(PlatformErrorCodes.LOGIN_BAD_CREDENTIALS, 401, message, cause);
    }
}
