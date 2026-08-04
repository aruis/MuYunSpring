package net.ximatai.muyun.spring.dynamic.runtime;

public interface DynamicActionExecutor {
    String executorKey();

    /**
     * Exposes an executor to configuration only when an implementation explicitly overrides
     * this internal default with a bindable definition.
     */
    default DynamicActionExecutorDefinition definition() {
        return DynamicActionExecutorDefinition.internal(executorKey());
    }

    Object execute(DynamicActionExecutionContext context, DynamicActionExecutionRequest request);

    default Object execute(DynamicActionExecutionContext context,
                           DynamicActionExecutionRequest request,
                           DynamicActionOperations operations) {
        return execute(context, request);
    }
}
