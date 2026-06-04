package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.platform.MenuVisibilityPolicyService;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
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
        return roleService.hasActionPermission(user.userId(), moduleAlias, PlatformAction.VIEW.code());
    }
}
