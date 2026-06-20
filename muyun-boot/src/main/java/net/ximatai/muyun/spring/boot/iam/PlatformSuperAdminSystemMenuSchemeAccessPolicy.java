package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.platform.menu.SystemMenuSchemeAccessPolicy;

import java.util.Objects;

public class PlatformSuperAdminSystemMenuSchemeAccessPolicy implements SystemMenuSchemeAccessPolicy {
    @Override
    public boolean canUseSystemMenuScheme(CurrentUser user) {
        return user != null && Objects.equals(user.userId(), UserAccountService.PLATFORM_SUPER_ADMIN_USER_ID);
    }
}
