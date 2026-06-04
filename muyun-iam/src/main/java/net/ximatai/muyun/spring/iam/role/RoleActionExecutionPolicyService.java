package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.platform.ActionAccessMode;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.ActionDefaultPolicy;
import org.springframework.stereotype.Service;

@Service
public class RoleActionExecutionPolicyService implements ActionExecutionPolicyService {
    private final RoleService roleService;

    public RoleActionExecutionPolicyService(RoleService roleService) {
        this.roleService = roleService;
    }

    @Override
    public void requireAuthorized(ActionExecutionContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (context.actionPolicy().accessMode() == ActionAccessMode.ANONYMOUS_ALLOWED) {
            return;
        }
        CurrentUser currentUser = context.currentUser()
                .orElseThrow(() -> new PlatformException("action requires current user"));
        if (currentUser.system()) {
            return;
        }
        if (context.actionPolicy().accessMode() == ActionAccessMode.LOGIN_REQUIRED
                || !context.actionPolicy().actionAuth()
                || context.actionPolicy().defaultPolicy() == ActionDefaultPolicy.AUTHENTICATED_USER) {
            return;
        }
        String permissionActionCode = context.actionPolicy().permissionActionCode();
        if (roleService.hasActionPermission(currentUser.userId(), context.moduleAlias(), permissionActionCode)) {
            return;
        }
        throw new PlatformException("action permission denied: " + context.permissionCode());
    }
}
