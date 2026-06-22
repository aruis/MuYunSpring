package net.ximatai.muyun.spring.common.exception;

import java.util.List;
import java.util.Map;

public class PlatformException extends RuntimeException {
    private final String code;
    private final int httpStatus;
    private final ErrorScope scope;
    private final List<ErrorTarget> targets;
    private final Map<String, Object> details;

    public PlatformException(String message) {
        this(PlatformErrorCodes.VALIDATION_FAILED, 400, message);
    }

    public PlatformException(String message, Throwable cause) {
        this(PlatformErrorCodes.VALIDATION_FAILED, 400, message, cause);
    }

    public PlatformException(String code, int httpStatus, String message) {
        this(code, httpStatus, message, ErrorScope.empty(), List.of(), Map.of());
    }

    public PlatformException(String code, int httpStatus, String message, Throwable cause) {
        this(code, httpStatus, message, cause, ErrorScope.empty(), List.of(), Map.of());
    }

    public PlatformException(String code,
                             int httpStatus,
                             String message,
                             ErrorScope scope,
                             List<ErrorTarget> targets,
                             Map<String, Object> details) {
        super(message);
        this.code = normalizeCode(code);
        this.httpStatus = httpStatus;
        this.scope = scope == null ? ErrorScope.empty() : scope;
        this.targets = targets == null ? List.of() : List.copyOf(targets);
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public PlatformException(String code,
                             int httpStatus,
                             String message,
                             Throwable cause,
                             ErrorScope scope,
                             List<ErrorTarget> targets,
                             Map<String, Object> details) {
        super(message, cause);
        this.code = normalizeCode(code);
        this.httpStatus = httpStatus;
        this.scope = scope == null ? ErrorScope.empty() : scope;
        this.targets = targets == null ? List.of() : List.copyOf(targets);
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public String code() {
        return code;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public ErrorScope scope() {
        return scope;
    }

    public List<ErrorTarget> targets() {
        return targets;
    }

    public Map<String, Object> details() {
        return details;
    }

    private static String normalizeCode(String code) {
        return code == null || code.isBlank() ? PlatformErrorCodes.VALIDATION_FAILED : code;
    }
}
