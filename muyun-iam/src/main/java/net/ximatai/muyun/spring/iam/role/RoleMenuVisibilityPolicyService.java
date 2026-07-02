package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.platform.MenuVisibilityPolicyService;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import jakarta.enterprise.context.Dependent;

import java.util.Optional;

@Dependent
public class RoleMenuVisibilityPolicyService implements MenuVisibilityPolicyService {
    private final RoleService roleService;

    public RoleMenuVisibilityPolicyService(RoleService roleService) {
        this.roleService = roleService;
    }

    @Override
    public boolean canViewModuleMenu(String moduleAlias, Optional<CurrentUser> currentUser) {
        CurrentUser user = currentUser.orElse(null);
        if (user == null) {
            return false;
        }
        if (user.system()) {
            return true;
        }
        return roleService.hasActionPermission(user.userId(), moduleAlias, PlatformAction.MENU.code());
    }
}
