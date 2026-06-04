package net.ximatai.muyun.spring.common.platform;

import net.ximatai.muyun.spring.common.identity.CurrentUser;

import java.util.Optional;

public interface MenuVisibilityPolicyService {
    boolean canViewModuleMenu(String moduleAlias, Optional<CurrentUser> currentUser);

    static MenuVisibilityPolicyService denyAll() {
        return (moduleAlias, currentUser) -> false;
    }
}
