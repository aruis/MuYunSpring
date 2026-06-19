package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataDeclaration;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataDeclarationProvider;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuScheme;
import net.ximatai.muyun.spring.platform.menu.MenuSchemeService;
import net.ximatai.muyun.spring.platform.menu.MenuScopeType;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import net.ximatai.muyun.spring.platform.menu.MenuType;

import java.util.List;

public class PlatformAdminMenuInitialDataDeclarationProvider implements InitialDataDeclarationProvider {
    public static final String ADMIN_SCHEME_ID = "platform.menu_scheme.admin";
    public static final String ADMIN_SCHEME_ALIAS = "platform_admin";

    private final MenuSchemeService schemeService;
    private final MenuService menuService;

    public PlatformAdminMenuInitialDataDeclarationProvider(MenuSchemeService schemeService, MenuService menuService) {
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
    public List<InitialDataDeclaration<?>> declarations() {
        return List.of(
                adminScheme(),
                group(PlatformMenuGroups.CONFIG, "平台配置与低代码运维", 10),
                group(PlatformMenuGroups.IDENTITY, "组织与权限", 20),
                group(PlatformMenuGroups.OPS, "平台运行运维", 30)
        );
    }

    private InitialDataDeclaration<MenuScheme> adminScheme() {
        MenuScheme desired = new MenuScheme();
        desired.setId(ADMIN_SCHEME_ID);
        desired.setAlias(ADMIN_SCHEME_ALIAS);
        desired.setScopeType(MenuScopeType.SYSTEM);
        desired.setScopeId(MenuSchemeService.SYSTEM_SCOPE_ID);
        desired.setTitle("平台超管");
        desired.setEnabled(Boolean.TRUE);
        desired.setSortOrder(1);
        return InitialDataDeclaration.reconcileManaged(schemeService, desired);
    }

    private InitialDataDeclaration<Menu> group(String id, String title, int sortOrder) {
        Menu desired = new Menu();
        desired.setId(id);
        desired.setSchemeId(ADMIN_SCHEME_ID);
        desired.setParentId(TreeAbility.ROOT_ID);
        desired.setMenuType(MenuType.GROUP);
        desired.setTitle(title);
        desired.setEnabled(Boolean.TRUE);
        desired.setSortOrder(sortOrder);
        return InitialDataDeclaration.reconcileManaged(menuService, desired);
    }
}
