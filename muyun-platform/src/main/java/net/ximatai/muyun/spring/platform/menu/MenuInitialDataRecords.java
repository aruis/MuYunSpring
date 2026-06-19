package net.ximatai.muyun.spring.platform.menu;

import net.ximatai.muyun.spring.platform.initialdata.InitialDataField;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataPolicy;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataRecord;

public final class MenuInitialDataRecords {
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

    private MenuInitialDataRecords() {
    }

    public static InitialDataRecord<MenuScheme> systemScheme(MenuScheme existing, MenuScheme desired) {
        return InitialDataRecord
                .of(desired.getId(), InitialDataPolicy.RECONCILE_MANAGED, existing, desired)
                .identity(SCHEME_ID, SCHEME_ALIAS, SCHEME_SCOPE_TYPE, SCHEME_SCOPE_ID, SCHEME_TENANT_ID)
                .operator(SCHEME_TITLE, SCHEME_ENABLED, SCHEME_SORT_ORDER);
    }

    public static InitialDataRecord<Menu> group(Menu existing, Menu desired) {
        return menuNode(existing, desired);
    }

    public static InitialDataRecord<Menu> module(Menu existing, Menu desired) {
        return menuNode(existing, desired);
    }

    private static InitialDataRecord<Menu> menuNode(Menu existing, Menu desired) {
        return InitialDataRecord
                .of(desired.getId(), InitialDataPolicy.RECONCILE_MANAGED, existing, desired)
                .identity(MENU_ID, MENU_SCHEME_ID)
                .managed(MENU_PARENT_ID, MENU_TYPE, MENU_MODULE_ALIAS, MENU_ROUTE, MENU_EXTERNAL_URL,
                        MENU_PAGE_MODE, MENU_DEFAULT_UI_CONFIG_ID, MENU_DEFAULT_QUERY_TEMPLATE_ID,
                        MENU_ENTRY_PARAMS_JSON)
                .operator(MENU_TITLE, MENU_ENABLED, MENU_SORT_ORDER);
    }
}
