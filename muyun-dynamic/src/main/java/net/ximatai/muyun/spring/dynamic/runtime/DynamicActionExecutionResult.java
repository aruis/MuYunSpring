package net.ximatai.muyun.spring.dynamic.runtime;

public record DynamicActionExecutionResult(
        DynamicActionExecutionContext context,
        Object value
) {
}
