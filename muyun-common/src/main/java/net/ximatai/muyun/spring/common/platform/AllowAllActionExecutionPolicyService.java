package net.ximatai.muyun.spring.common.platform;

public class AllowAllActionExecutionPolicyService implements ActionExecutionPolicyService {
    @Override
    public void requireAuthorized(ActionExecutionContext context) {
        if (context == null) {
            throw new IllegalArgumentException("action execution context must not be null");
        }
    }

    @Override
    public ActionAuthorizationResult authorize(ActionExecutionContext context) {
        requireAuthorized(context);
        return ActionAuthorizationResult.allowed(context, "ALLOW_ALL");
    }
}
