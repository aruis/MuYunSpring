package net.ximatai.muyun.spring.dynamic.runtime;

public class DynamicActionExecutionException extends RuntimeException {
    private final DynamicActionExecutionContext context;

    public DynamicActionExecutionException(String message, DynamicActionExecutionContext context) {
        super(message);
        this.context = context;
    }

    public DynamicActionExecutionContext context() {
        return context;
    }
}
