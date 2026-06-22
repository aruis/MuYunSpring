package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.platform.menu.MenuService;

public final class PlatformMenuGroups {
    public static final String PLATFORM = MenuService.ADMIN_PLATFORM_GROUP_ID;
    public static final String CONFIG = MenuService.ADMIN_CONFIG_GROUP_ID;
    public static final String IDENTITY = MenuService.ADMIN_IDENTITY_GROUP_ID;
    public static final String OPS = MenuService.ADMIN_OPS_GROUP_ID;

    private PlatformMenuGroups() {
    }
}
