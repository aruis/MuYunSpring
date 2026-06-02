package net.ximatai.muyun.spring.dynamic.runtime;

public class DynamicActionExecutionException extends RuntimeException {
    public static final String STAGE_AVAILABILITY = "availability";
    public static final String STAGE_BEFORE_EXECUTE_RULE = "beforeExecuteRule";
    public static final String STAGE_EXECUTE = "execute";

    private final DynamicActionExecutionContext context;
    private final String failureStage;

    public DynamicActionExecutionException(String message, DynamicActionExecutionContext context) {
        this(message, context, STAGE_EXECUTE, null);
    }

    public DynamicActionExecutionException(String message, DynamicActionExecutionContext context, Throwable cause) {
        this(message, context, STAGE_EXECUTE, cause);
    }

    public DynamicActionExecutionException(String message,
                                           DynamicActionExecutionContext context,
                                           String failureStage,
                                           Throwable cause) {
        super(message, cause);
        this.context = context;
        this.failureStage = normalizeStage(failureStage);
    }

    public DynamicActionExecutionContext context() {
        return context;
    }

    public String failureStage() {
        return failureStage;
    }

    private static String normalizeStage(String value) {
        return value == null || value.isBlank() ? STAGE_EXECUTE : value.trim();
    }
}
