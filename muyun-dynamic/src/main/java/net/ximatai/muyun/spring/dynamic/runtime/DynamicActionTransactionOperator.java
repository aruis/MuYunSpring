package net.ximatai.muyun.spring.dynamic.runtime;

import java.util.function.Supplier;

@FunctionalInterface
public interface DynamicActionTransactionOperator {
    DynamicActionTransactionOperator NONE = (context, action) -> action.get();

    Object execute(DynamicActionExecutionContext context, Supplier<?> action);

    @SuppressWarnings("unchecked")
    default <T> T executeResult(DynamicActionExecutionContext context, Supplier<T> action) {
        return (T) execute(context, action);
    }

    static DynamicActionTransactionOperator none() {
        return NONE;
    }
}
