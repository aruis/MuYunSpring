package net.ximatai.muyun.spring.dynamic.runtime;

public record DynamicActionExecutionResult(
        DynamicActionExecutionContext context,
        Object value,
        DynamicActionResultBody body
) {
    public DynamicActionExecutionResult(DynamicActionExecutionContext context, Object value) {
        this(context, value, DynamicActionResultBody.of(value));
    }

    public DynamicActionExecutionResult {
        body = body == null ? DynamicActionResultBody.of(value) : body;
    }
}
