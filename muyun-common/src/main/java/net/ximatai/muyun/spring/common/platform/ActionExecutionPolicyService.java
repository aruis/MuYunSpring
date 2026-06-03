package net.ximatai.muyun.spring.common.platform;

public interface ActionExecutionPolicyService {
    void requireAuthorized(ActionExecutionContext context);
}
