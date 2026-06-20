package net.ximatai.muyun.spring.platform.menu;

import net.ximatai.muyun.spring.common.identity.CurrentUser;

@FunctionalInterface
public interface SystemMenuSchemeAccessPolicy {
    SystemMenuSchemeAccessPolicy DENY_ALL = user -> false;

    boolean canUseSystemMenuScheme(CurrentUser user);
}
