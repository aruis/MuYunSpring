package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.platform.ActionAccessMode;
import net.ximatai.muyun.spring.common.platform.ActionAuthorizationResult;
import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicyService;
import org.springframework.stereotype.Service;

@Service
public class RoleActionExecutionPolicyService implements ActionExecutionPolicyService {
    public static final String DECISION_ANONYMOUS_ALLOWED = "ANONYMOUS_ALLOWED";
    public static final String DECISION_SYSTEM_USER = "SYSTEM_USER";
    public static final String DECISION_LOGIN_REQUIRED = "LOGIN_REQUIRED";
    public static final String DECISION_ACTION_AUTH_DISABLED = "ACTION_AUTH_DISABLED";
    public static final String DECISION_ACTION_DEFAULT_GRANT = "ACTION_DEFAULT_GRANT";
    public static final String DECISION_ROLE_GRANTED = "ROLE_GRANTED";

    private final RoleService roleService;

    public RoleActionExecutionPolicyService(RoleService roleService) {
        this.roleService = roleService;
    }

    @Override
    public void requireAuthorized(ActionExecutionContext context) {
        authorize(context);
    }

    @Override
    public ActionAuthorizationResult authorize(ActionExecutionContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (context.actionPolicy().accessMode() == ActionAccessMode.ANONYMOUS_ALLOWED) {
            return ActionAuthorizationResult.allowed(context, DECISION_ANONYMOUS_ALLOWED);
        }
        CurrentUser currentUser = context.currentUser()
                .orElseThrow(() -> new PlatformException("action requires current user"));
        if (currentUser.system()) {
            return ActionAuthorizationResult.allowed(context, DECISION_SYSTEM_USER);
        }
        if (context.actionPolicy().accessMode() == ActionAccessMode.LOGIN_REQUIRED) {
            return ActionAuthorizationResult.allowed(context, DECISION_LOGIN_REQUIRED);
        }
        if (!context.actionPolicy().actionAuth()) {
            return ActionAuthorizationResult.allowed(context, DECISION_ACTION_AUTH_DISABLED);
        }
        if (grantsAuthenticatedUser(context.actionPolicy().defaultGrantPolicy())) {
            return ActionAuthorizationResult.allowed(context, DECISION_ACTION_DEFAULT_GRANT);
        }
        String permissionActionCode = context.actionPolicy().permissionActionCode();
        if (roleService.hasActionPermission(currentUser.userId(), context.moduleAlias(), permissionActionCode)) {
            return ActionAuthorizationResult.allowed(context, DECISION_ROLE_GRANTED);
        }
        throw new PlatformException("action permission denied: " + context.permissionCode());
    }

    private boolean grantsAuthenticatedUser(ActionDefaultGrantPolicy policy) {
        return policy != null && policy.grantsAuthenticatedUser();
    }
}
