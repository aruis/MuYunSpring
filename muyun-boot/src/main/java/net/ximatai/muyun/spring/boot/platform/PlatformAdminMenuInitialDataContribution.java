package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataContext;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataContribution;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataField;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataPolicy;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataRecord;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuScheme;
import net.ximatai.muyun.spring.platform.menu.MenuSchemeService;
import net.ximatai.muyun.spring.platform.menu.MenuScopeType;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import net.ximatai.muyun.spring.platform.menu.MenuType;

public class PlatformAdminMenuInitialDataContribution implements InitialDataContribution {
    public static final String ADMIN_SCHEME_ID = "platform.menu_scheme.admin";
    public static final String ADMIN_SCHEME_ALIAS = "platform_admin";

    private static final InitialDataField<MenuScheme> SCHEME_ID =
            InitialDataField.of("id", MenuScheme::getId, MenuScheme::setId);
    private static final InitialDataField<MenuScheme> SCHEME_ALIAS =
            InitialDataField.of("alias", MenuScheme::getAlias, MenuScheme::setAlias);
    private static final InitialDataField<MenuScheme> SCHEME_SCOPE_TYPE =
            InitialDataField.of("scopeType", MenuScheme::getScopeType, MenuScheme::setScopeType);
    private static final InitialDataField<MenuScheme> SCHEME_SCOPE_ID =
            InitialDataField.of("scopeId", MenuScheme::getScopeId, MenuScheme::setScopeId);
    private static final InitialDataField<MenuScheme> SCHEME_TENANT_ID =
            InitialDataField.of("tenantId", MenuScheme::getTenantId, MenuScheme::setTenantId);
    private static final InitialDataField<MenuScheme> SCHEME_TITLE =
            InitialDataField.of("title", MenuScheme::getTitle, MenuScheme::setTitle);
    private static final InitialDataField<MenuScheme> SCHEME_ENABLED =
            InitialDataField.of("enabled", MenuScheme::getEnabled, MenuScheme::setEnabled);
    private static final InitialDataField<MenuScheme> SCHEME_SORT_ORDER =
            InitialDataField.of("sortOrder", MenuScheme::getSortOrder, MenuScheme::setSortOrder);

    private static final InitialDataField<Menu> MENU_ID =
            InitialDataField.of("id", Menu::getId, Menu::setId);
    private static final InitialDataField<Menu> MENU_SCHEME_ID =
            InitialDataField.of("schemeId", Menu::getSchemeId, Menu::setSchemeId);
    private static final InitialDataField<Menu> MENU_PARENT_ID =
            InitialDataField.of("parentId", Menu::getParentId, Menu::setParentId);
    private static final InitialDataField<Menu> MENU_TYPE =
            InitialDataField.of("menuType", Menu::getMenuType, Menu::setMenuType);
    private static final InitialDataField<Menu> MENU_MODULE_ALIAS =
            InitialDataField.of("moduleAlias", Menu::getModuleAlias, Menu::setModuleAlias);
    private static final InitialDataField<Menu> MENU_ROUTE =
            InitialDataField.of("route", Menu::getRoute, Menu::setRoute);
    private static final InitialDataField<Menu> MENU_EXTERNAL_URL =
            InitialDataField.of("externalUrl", Menu::getExternalUrl, Menu::setExternalUrl);
    private static final InitialDataField<Menu> MENU_PAGE_MODE =
            InitialDataField.of("pageMode", Menu::getPageMode, Menu::setPageMode);
    private static final InitialDataField<Menu> MENU_DEFAULT_UI_CONFIG_ID =
            InitialDataField.of("defaultUiConfigId", Menu::getDefaultUiConfigId, Menu::setDefaultUiConfigId);
    private static final InitialDataField<Menu> MENU_DEFAULT_QUERY_TEMPLATE_ID =
            InitialDataField.of("defaultQueryTemplateId", Menu::getDefaultQueryTemplateId,
                    Menu::setDefaultQueryTemplateId);
    private static final InitialDataField<Menu> MENU_ENTRY_PARAMS_JSON =
            InitialDataField.of("entryParamsJson", Menu::getEntryParamsJson, Menu::setEntryParamsJson);
    private static final InitialDataField<Menu> MENU_TITLE =
            InitialDataField.of("title", Menu::getTitle, Menu::setTitle);
    private static final InitialDataField<Menu> MENU_ENABLED =
            InitialDataField.of("enabled", Menu::getEnabled, Menu::setEnabled);
    private static final InitialDataField<Menu> MENU_SORT_ORDER =
            InitialDataField.of("sortOrder", Menu::getSortOrder, Menu::setSortOrder);

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

        context.apply(InitialDataRecord
                        .of(ADMIN_SCHEME_ID, InitialDataPolicy.RECONCILE_MANAGED,
                                schemeService.selectIgnoreSoftDelete(ADMIN_SCHEME_ID), desired)
                        .identity(SCHEME_ID, SCHEME_ALIAS, SCHEME_SCOPE_TYPE, SCHEME_SCOPE_ID, SCHEME_TENANT_ID)
                        .operator(SCHEME_TITLE, SCHEME_ENABLED, SCHEME_SORT_ORDER),
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

        context.apply(InitialDataRecord
                        .of(id, InitialDataPolicy.RECONCILE_MANAGED, menuService.selectIgnoreSoftDelete(id), desired)
                        .identity(MENU_ID, MENU_SCHEME_ID)
                        .managed(MENU_PARENT_ID, MENU_TYPE, MENU_MODULE_ALIAS, MENU_ROUTE, MENU_EXTERNAL_URL,
                                MENU_PAGE_MODE, MENU_DEFAULT_UI_CONFIG_ID, MENU_DEFAULT_QUERY_TEMPLATE_ID,
                                MENU_ENTRY_PARAMS_JSON)
                        .operator(MENU_TITLE, MENU_ENABLED, MENU_SORT_ORDER),
                menu -> menuService.insert(menu),
                menu -> menuService.update(menu));
    }

    static InitialDataField<Menu> menuIdField() {
        return MENU_ID;
    }

    static InitialDataField<Menu> menuSchemeIdField() {
        return MENU_SCHEME_ID;
    }

    static InitialDataField<Menu> menuParentIdField() {
        return MENU_PARENT_ID;
    }

    static InitialDataField<Menu> menuTypeField() {
        return MENU_TYPE;
    }

    static InitialDataField<Menu> menuModuleAliasField() {
        return MENU_MODULE_ALIAS;
    }

    static InitialDataField<Menu> menuRouteField() {
        return MENU_ROUTE;
    }

    static InitialDataField<Menu> menuExternalUrlField() {
        return MENU_EXTERNAL_URL;
    }

    static InitialDataField<Menu> menuPageModeField() {
        return MENU_PAGE_MODE;
    }

    static InitialDataField<Menu> menuDefaultUiConfigIdField() {
        return MENU_DEFAULT_UI_CONFIG_ID;
    }

    static InitialDataField<Menu> menuDefaultQueryTemplateIdField() {
        return MENU_DEFAULT_QUERY_TEMPLATE_ID;
    }

    static InitialDataField<Menu> menuEntryParamsJsonField() {
        return MENU_ENTRY_PARAMS_JSON;
    }

    static InitialDataField<Menu> menuTitleField() {
        return MENU_TITLE;
    }

    static InitialDataField<Menu> menuEnabledField() {
        return MENU_ENABLED;
    }

    static InitialDataField<Menu> menuSortOrderField() {
        return MENU_SORT_ORDER;
    }
}
