package net.ximatai.muyun.spring.common.platform;

public interface ActionExecutionPolicyService {
    void requireAuthorized(ActionExecutionContext context);

    default ActionAuthorizationResult authorize(ActionExecutionContext context) {
        requireAuthorized(context);
        return ActionAuthorizationResult.allowed(context);
    }

    default void requireRecordAction(ActionExecutionContext context) {
        if (context == null) {
            throw new IllegalArgumentException("action execution context must not be null");
        }
        if (!context.hasRecordContext()) {
            throw new IllegalArgumentException("record action requires record id context: "
                    + context.moduleAlias() + "." + context.actionCode());
        }
        authorize(context);
    }
}
