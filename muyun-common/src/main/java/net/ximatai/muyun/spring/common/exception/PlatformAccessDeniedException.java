package net.ximatai.muyun.spring.common.exception;

import java.util.List;
import java.util.Map;

public class PlatformAccessDeniedException extends PlatformException {
    public PlatformAccessDeniedException(String message) {
        super(PlatformErrorCodes.ACCESS_DENIED, 403, message);
    }

    public PlatformAccessDeniedException(String message, Throwable cause) {
        super(PlatformErrorCodes.ACCESS_DENIED, 403, message, cause);
    }

    public PlatformAccessDeniedException(String message, ErrorScope scope) {
        super(PlatformErrorCodes.ACCESS_DENIED, 403, message, scope, List.of(), Map.of());
    }
}
