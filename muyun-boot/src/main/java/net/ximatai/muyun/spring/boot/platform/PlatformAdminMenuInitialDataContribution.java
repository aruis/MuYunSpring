package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataContext;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataContribution;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuInitialDataRecords;
import net.ximatai.muyun.spring.platform.menu.MenuScheme;
import net.ximatai.muyun.spring.platform.menu.MenuSchemeService;
import net.ximatai.muyun.spring.platform.menu.MenuScopeType;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import net.ximatai.muyun.spring.platform.menu.MenuType;

public class PlatformAdminMenuInitialDataContribution implements InitialDataContribution {
    public static final String ADMIN_SCHEME_ID = "platform.menu_scheme.admin";
    public static final String ADMIN_SCHEME_ALIAS = "platform_admin";

    private final MenuSchemeService schemeService;
    private final MenuService menuService;

    public PlatformAdminMenuInitialDataContribution(MenuSchemeService schemeService, MenuService menuService) {
        this.schemeService = schemeService;
        this.menuService = menuService;
    }

    @Override
    public String name() {
        return "platform.admin-menu";
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public void contribute(InitialDataContext context) {
        ensureAdminScheme(context);
        ensureGroup(context, PlatformMenuGroups.CONFIG, "平台配置与低代码运维", 10);
        ensureGroup(context, PlatformMenuGroups.IDENTITY, "组织与权限", 20);
        ensureGroup(context, PlatformMenuGroups.OPS, "平台运行运维", 30);
    }

    private void ensureAdminScheme(InitialDataContext context) {
        MenuScheme desired = new MenuScheme();
        desired.setId(ADMIN_SCHEME_ID);
        desired.setAlias(ADMIN_SCHEME_ALIAS);
        desired.setScopeType(MenuScopeType.SYSTEM);
        desired.setScopeId(MenuSchemeService.SYSTEM_SCOPE_ID);
        desired.setTitle("平台超管");
        desired.setEnabled(Boolean.TRUE);
        desired.setSortOrder(1);

        context.apply(MenuInitialDataRecords.systemScheme(
                        schemeService.selectIgnoreSoftDelete(ADMIN_SCHEME_ID), desired),
                scheme -> schemeService.insert(scheme),
                scheme -> schemeService.update(scheme));
    }

    private void ensureGroup(InitialDataContext context, String id, String title, int sortOrder) {
        Menu desired = new Menu();
        desired.setId(id);
        desired.setSchemeId(ADMIN_SCHEME_ID);
        desired.setParentId(TreeAbility.ROOT_ID);
        desired.setMenuType(MenuType.GROUP);
        desired.setTitle(title);
        desired.setEnabled(Boolean.TRUE);
        desired.setSortOrder(sortOrder);

        context.apply(MenuInitialDataRecords.group(menuService.selectIgnoreSoftDelete(id), desired),
                menu -> menuService.insert(menu),
                menu -> menuService.update(menu));
    }
}
