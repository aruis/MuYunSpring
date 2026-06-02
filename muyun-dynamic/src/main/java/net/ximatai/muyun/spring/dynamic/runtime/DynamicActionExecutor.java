package net.ximatai.muyun.spring.dynamic.runtime;

public interface DynamicActionExecutor {
    String executorKey();

    Object execute(DynamicActionExecutionContext context, DynamicActionExecutionRequest request);

    default Object execute(DynamicActionExecutionContext context,
                           DynamicActionExecutionRequest request,
                           DynamicActionOperations operations) {
        return execute(context, request);
    }
}
