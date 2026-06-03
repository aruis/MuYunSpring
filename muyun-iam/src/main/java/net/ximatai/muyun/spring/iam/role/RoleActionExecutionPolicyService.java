package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicyService;
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
        CurrentUser currentUser = context.currentUser()
                .orElseThrow(() -> new PlatformException("action requires current user"));
        if (currentUser.system()) {
            return;
        }
        if (roleService.hasActionPermission(currentUser.userId(), context.moduleAlias(), context.actionCode())) {
            return;
        }
        throw new PlatformException("action permission denied: " + context.permissionCode());
    }
}
