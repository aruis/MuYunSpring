package net.ximatai.muyun.spring.common.exception;

public class PlatformConfigurationException extends PlatformException {
    public PlatformConfigurationException(String message) {
        super(PlatformErrorCodes.CONFIG_MISSING, 409, message);
    }

    public PlatformConfigurationException(String message, Throwable cause) {
        super(PlatformErrorCodes.CONFIG_MISSING, 409, message, cause);
    }

    public PlatformConfigurationException(String code, String message, ErrorScope scope) {
        super(code, 409, message, scope, java.util.List.of(), java.util.Map.of());
    }
}
