package net.ximatai.muyun.spring.platform.menu;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.common.exception.AuthenticationRequiredException;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.platform.MenuVisibilityPolicyService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataAbility;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataOptions;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import net.ximatai.muyun.spring.platform.module.ModuleEntryType;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.ui.PlatformQueryTemplate;
import net.ximatai.muyun.spring.platform.ui.PlatformQueryTemplateService;
import net.ximatai.muyun.spring.platform.ui.PlatformUiConfig;
import net.ximatai.muyun.spring.platform.ui.PlatformUiConfigService;
import net.ximatai.muyun.spring.platform.ui.PlatformUiSet;
import net.ximatai.muyun.spring.platform.ui.PlatformUiSetService;
import net.ximatai.muyun.spring.platform.ui.PlatformUiSetType;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.ability.query.QueryDescriptor;
import net.ximatai.muyun.spring.ability.query.QueryField;
import net.ximatai.muyun.spring.ability.query.QueryOperator;
import net.ximatai.muyun.spring.ability.query.QueryValueType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class MenuService extends AbstractAbilityService<Menu> implements
        SoftDeleteAbility<Menu>,
        EnableAbility<Menu>,
        TreeAbility<Menu>,
        InitialDataAbility<Menu>, QueryAbility<Menu>
{
    public static final String MODULE_ALIAS = "platform.menu";
    public static final String ADMIN_PLATFORM_GROUP_ID = "platform.menu.group.platform";
    public static final String ADMIN_CONFIG_GROUP_ID = "platform.menu.group.config";
    public static final String ADMIN_IDENTITY_GROUP_ID = "platform.menu.group.identity";
    public static final String ADMIN_OPS_GROUP_ID = "platform.menu.group.ops";

    private final MenuSchemeService schemeService;
    private final PlatformModuleService moduleService;
    private final MenuVisibilityPolicyService visibilityPolicyService;
    private final PlatformUiConfigService uiConfigService;
    private final PlatformUiSetService uiSetService;
    private final PlatformQueryTemplateService queryTemplateService;

    public MenuService(BaseDao<Menu, String> menuDao, MenuSchemeService schemeService, PlatformModuleService moduleService) {
        this(menuDao, schemeService, moduleService, Optional.empty(),
                (PlatformUiConfigService) null, null, null);
    }


    @Override
    public QueryDescriptor queryDescriptor() {
        return QueryDescriptor.builder(MODULE_ALIAS)
                .field(QueryField.of("id", QueryOperator.EQ, QueryOperator.IN).withTitle("ID"))
                .field(QueryField.of("schemeId", QueryOperator.EQ, QueryOperator.IN).withTitle("方案"))
                .field(QueryField.of("parentId", QueryOperator.EQ, QueryOperator.IN).withTitle("父ID"))
                .field(QueryField.of("title", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.LIKE)
                .withTitle("名称").withQuickSearch().withSortable())
                .field(QueryField.of("menuType", QueryOperator.EQ).withTitle("菜单类型"))
                .field(QueryField.of("moduleAlias", QueryOperator.EQ, QueryOperator.IN).withTitle("模块标识"))
                .field(QueryField.of("route", QueryOperator.EQ).withTitle("路由"))
                .field(QueryField.of("externalUrl", QueryOperator.EQ).withTitle("外部URL"))
                .field(QueryField.of("pageMode", QueryOperator.EQ).withTitle("页面模式"))
                .field(QueryField.of("defaultUiConfigId", QueryOperator.EQ, QueryOperator.IN).withTitle("默认UI配置"))
                .field(QueryField.of("defaultQueryTemplateId", QueryOperator.EQ, QueryOperator.IN).withTitle("默认查询模板"))
                .field(QueryField.of("enabled", QueryValueType.BOOLEAN, QueryOperator.EQ).withTitle("启用状态"))
                .field(QueryField.of("sortOrder", QueryValueType.INTEGER, QueryOperator.EQ)
                .withTitle("排序号").withSortable())
                .field(QueryField.of("createdAt", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                        QueryOperator.BETWEEN)
                .withTitle("创建时间").withSortable())
                .field(QueryField.of("updatedAt", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                        QueryOperator.BETWEEN)
                .withTitle("更新时间").withSortable())
                .defaultSort(Sort.asc("sortOrder"))
                .defaultSort(Sort.asc("title"))
                .build();
    }
    public MenuService(BaseDao<Menu, String> menuDao,
                       MenuSchemeService schemeService,
                       PlatformModuleService moduleService,
                       Optional<MenuVisibilityPolicyService> visibilityPolicyService) {
        this(menuDao, schemeService, moduleService, visibilityPolicyService,
                (PlatformUiConfigService) null, null, null);
    }

    @Autowired
    public MenuService(BaseDao<Menu, String> menuDao,
                       MenuSchemeService schemeService,
                       PlatformModuleService moduleService,
                       Optional<MenuVisibilityPolicyService> visibilityPolicyService,
                       ObjectProvider<PlatformUiConfigService> uiConfigServiceProvider,
                       ObjectProvider<PlatformUiSetService> uiSetServiceProvider,
                       ObjectProvider<PlatformQueryTemplateService> queryTemplateServiceProvider) {
        this(menuDao, schemeService, moduleService, visibilityPolicyService,
                uiConfigServiceProvider == null ? null : uiConfigServiceProvider.getIfAvailable(),
                uiSetServiceProvider == null ? null : uiSetServiceProvider.getIfAvailable(),
                queryTemplateServiceProvider == null ? null : queryTemplateServiceProvider.getIfAvailable());
    }

    public MenuService(BaseDao<Menu, String> menuDao,
                       MenuSchemeService schemeService,
                       PlatformModuleService moduleService,
                       Optional<MenuVisibilityPolicyService> visibilityPolicyService,
                       PlatformUiConfigService uiConfigService,
                       PlatformUiSetService uiSetService,
                       PlatformQueryTemplateService queryTemplateService) {
        super(MODULE_ALIAS, Menu.class, menuDao);
        this.schemeService = schemeService;
        this.moduleService = moduleService;
        this.visibilityPolicyService = visibilityPolicyService.orElseGet(MenuVisibilityPolicyService::denyAll);
        this.uiConfigService = uiConfigService;
        this.uiSetService = uiSetService;
        this.queryTemplateService = queryTemplateService;
    }

    @Override
    public void beforeInsert(Menu menu) {
        normalizeAndValidate(menu);
    }

    @Override
    public void beforeUpdate(Menu menu) {
        validateImmutableScheme(menu);
        normalizeAndValidate(menu);
    }

    @Override
    public InitialDataOptions initialDataOptions() {
        return InitialDataOptions.system("platform.admin-menu-groups", 11);
    }

    @Override
    public List<Menu> initialData() {
        return List.of(
                group(ADMIN_PLATFORM_GROUP_ID, TreeAbility.ROOT_ID, "平台管理", 10),
                group(ADMIN_CONFIG_GROUP_ID, ADMIN_PLATFORM_GROUP_ID, "平台配置与低代码运维", 10),
                group(ADMIN_IDENTITY_GROUP_ID, ADMIN_PLATFORM_GROUP_ID, "组织与权限", 20),
                group(ADMIN_OPS_GROUP_ID, ADMIN_PLATFORM_GROUP_ID, "平台运行运维", 30)
        );
    }

    private Menu group(String id, String parentId, String title, int sortOrder) {
        Menu menu = new Menu();
        menu.setId(id);
        menu.setSchemeId(MenuSchemeService.ADMIN_SCHEME_ID);
        menu.setParentId(parentId);
        menu.setMenuType(MenuType.GROUP);
        menu.setTitle(title);
        menu.setEnabled(Boolean.TRUE);
        menu.setSortOrder(sortOrder);
        return menu;
    }

    @Override
    public Criteria sortScope(Menu menu) {
        return scopedTreeCriteria(schemeScope(menu.getSchemeId()), menu.getParentId());
    }

    @Override
    public void validateSortScope(Menu left, Menu right) {
        if (!Objects.equals(left.getSchemeId(), right.getSchemeId())) {
            throw new PlatformException("Menu sort can only move records within the same scheme");
        }
        TreeAbility.super.validateSortScope(left, right);
    }

    @Override
    public List<Menu> children(String parentId) {
        if (TreeAbility.ROOT_ID.equals(parentId)) {
            rejectRootChildrenLookup("rootMenus(schemeId)");
        }
        return TreeAbility.super.children(parentId);
    }

    public List<Menu> rootMenus(String schemeId) {
        return children(schemeId, TreeAbility.ROOT_ID);
    }

    public List<Menu> children(String schemeId, String parentId) {
        if (schemeId == null || schemeId.isBlank() || parentId == null || parentId.isBlank()) {
            return List.of();
        }
        if (isSystemScheme(schemeId)) {
            try (TenantContext.Scope ignored = TenantContext.bypassTenantFilter("read system menu scheme")) {
                return TreeAbility.super.children(schemeScope(schemeId), parentId);
            }
        }
        return TreeAbility.super.children(schemeScope(schemeId), parentId);
    }

    public List<Menu> visibleRootMenus(String schemeId) {
        return visibleChildren(schemeId, TreeAbility.ROOT_ID, new LinkedHashSet<>());
    }

    public List<Menu> currentUserVisibleRootMenus() {
        CurrentUser user = CurrentUserContext.currentUser()
                .orElseThrow(() -> new AuthenticationRequiredException("current user is required"));
        MenuScheme scheme = schemeService.resolveCurrentUserScheme(user);
        return visibleRootMenus(scheme.getId());
    }

    public Menu currentUserVisibleMenu(String menuId) {
        if (menuId == null || menuId.isBlank()) {
            throw new PlatformException("menuId is required");
        }
        CurrentUser user = CurrentUserContext.currentUser()
                .orElseThrow(() -> new AuthenticationRequiredException("current user is required"));
        MenuScheme scheme = schemeService.resolveCurrentUserScheme(user);
        return findVisibleMenu(scheme.getId(), TreeAbility.ROOT_ID, menuId, new LinkedHashSet<>());
    }

    public Menu currentUserVisibleModuleMenu(String moduleAlias) {
        String validAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        CurrentUser user = CurrentUserContext.currentUser()
                .orElseThrow(() -> new AuthenticationRequiredException("current user is required"));
        MenuScheme scheme = schemeService.resolveCurrentUserScheme(user);
        return findVisibleModuleMenu(scheme.getId(), TreeAbility.ROOT_ID, validAlias, new LinkedHashSet<>());
    }

    public List<Menu> visibleChildren(String schemeId, String parentId) {
        return visibleChildren(schemeId, parentId, new LinkedHashSet<>());
    }

    private List<Menu> visibleChildren(String schemeId, String parentId, Set<String> visiting) {
        return children(schemeId, parentId).stream()
                .filter(menu -> isVisibleMenu(schemeId, menu, visiting))
                .toList();
    }

    private boolean isVisibleMenu(String schemeId, Menu menu, Set<String> visiting) {
        if (menu == null || !Boolean.TRUE.equals(menu.getEnabled())) {
            return false;
        }
        if (!visiting.add(menu.getId())) {
            return false;
        }
        MenuType type = menu.getMenuType() == null ? MenuType.GROUP : menu.getMenuType();
        try {
            if (isModuleEntryMenu(menu)) {
                return visibilityPolicyService.canViewModuleMenu(menu.getModuleAlias(), CurrentUserContext.currentUser());
            }
            if (type == MenuType.GROUP) {
                return !visibleChildren(schemeId, menu.getId(), visiting).isEmpty();
            }
            return true;
        } finally {
            visiting.remove(menu.getId());
        }
    }

    private Menu findVisibleMenu(String schemeId, String parentId, String targetMenuId, Set<String> visiting) {
        for (Menu menu : visibleChildren(schemeId, parentId, visiting)) {
            if (Objects.equals(menu.getId(), targetMenuId)) {
                return menu;
            }
            Menu found = findVisibleMenu(schemeId, menu.getId(), targetMenuId, visiting);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private Menu findVisibleModuleMenu(String schemeId, String parentId, String moduleAlias, Set<String> visiting) {
        for (Menu menu : visibleChildren(schemeId, parentId, visiting)) {
            if (isModuleEntryMenu(menu, moduleAlias)) {
                return menu;
            }
            Menu found = findVisibleModuleMenu(schemeId, menu.getId(), moduleAlias, visiting);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private boolean isModuleEntryMenu(Menu menu, String moduleAlias) {
        return isModuleEntryMenu(menu)
                && Objects.equals(menu.getModuleAlias(), moduleAlias);
    }

    private boolean isModuleEntryMenu(Menu menu) {
        return menu.getMenuType() != MenuType.GROUP && hasText(menu.getModuleAlias());
    }

    private void normalizeAndValidate(Menu menu) {
        MenuScheme scheme = requireScheme(menu.getSchemeId());
        menu.setTenantId(scheme.getTenantId());
        if (menu.getMenuType() == null) {
            menu.setMenuType(MenuType.GROUP);
        }
        normalizeTarget(menu);
        validateParentScheme(menu);
    }

    private MenuScheme requireScheme(String schemeId) {
        if (schemeId == null || schemeId.isBlank()) {
            throw new PlatformException("Menu requires schemeId");
        }
        MenuScheme scheme = schemeService.select(schemeId);
        if (scheme == null) {
            throw new PlatformException("Menu requires existing scheme: " + schemeId);
        }
        return scheme;
    }

    private void normalizeTarget(Menu menu) {
        switch (menu.getMenuType()) {
            case GROUP -> {
                requireNoOpenMode(menu);
                requireBlank(menu.getModuleAlias(), "GROUP menu cannot have moduleAlias");
                requireBlank(menu.getRoute(), "GROUP menu cannot have route");
                requireBlank(menu.getExternalUrl(), "GROUP menu cannot have externalUrl");
                requireBlankEntry(menu, "GROUP menu cannot have low-code entry config");
            }
            case MODULE -> {
                requireOpenMode(menu);
                PlatformModule module = requireModuleEntry(menu);
                syncModuleEntry(menu, module);
            }
            case ROUTE -> {
                requireOpenMode(menu);
                PlatformModule module = requireModuleEntry(menu);
                syncModuleEntry(menu, module);
            }
            case LINK -> {
                requireOpenMode(menu);
                PlatformModule module = requireModuleEntry(menu);
                syncModuleEntry(menu, module);
            }
        }
    }

    private void requireOpenMode(Menu menu) {
        if (menu.getOpenMode() == null) {
            throw new PlatformException(menu.getMenuType() + " menu requires openMode");
        }
    }

    private void requireNoOpenMode(Menu menu) {
        if (menu.getOpenMode() != null) {
            throw new PlatformException("GROUP menu cannot have openMode");
        }
    }

    private void normalizeModuleEntry(Menu menu, String moduleAlias) {
        if (menu.getPageMode() == null) {
            menu.setPageMode(MenuPageMode.LIST);
        }
        validateDefaultUiConfig(menu, moduleAlias);
        validateDefaultQueryTemplate(menu, moduleAlias);
    }

    private PlatformModule requireModuleEntry(Menu menu) {
        requireText(menu.getModuleAlias(), menu.getMenuType() + " menu requires moduleAlias");
        String moduleAlias = PlatformNameRules.requireModuleAlias(menu.getModuleAlias());
        PlatformModule module = moduleService.resolveVisibleModule(moduleAlias);
        if (module == null) {
            throw new PlatformException(menu.getMenuType() + " menu requires existing module: " + moduleAlias);
        }
        menu.setModuleAlias(moduleAlias);
        return module;
    }

    private void syncModuleEntry(Menu menu, PlatformModule module) {
        ModuleEntryType entryType = module.getEntryType() == null ? ModuleEntryType.MODULE : module.getEntryType();
        menu.setMenuType(menuType(entryType));
        switch (entryType) {
            case MODULE -> {
                menu.setRoute(null);
                menu.setExternalUrl(null);
                normalizeModuleEntry(menu, module.getAlias());
            }
            case ROUTE -> {
                menu.setRoute(module.getEntryRoute());
                menu.setExternalUrl(null);
                clearLowCodeEntry(menu);
            }
            case LINK -> {
                menu.setRoute(null);
                menu.setExternalUrl(module.getEntryExternalUrl());
                clearLowCodeEntry(menu);
            }
        }
    }

    private MenuType menuType(ModuleEntryType entryType) {
        return switch (entryType) {
            case MODULE -> MenuType.MODULE;
            case ROUTE -> MenuType.ROUTE;
            case LINK -> MenuType.LINK;
        };
    }

    private void clearLowCodeEntry(Menu menu) {
        menu.setPageMode(null);
        menu.setDefaultUiConfigId(null);
        menu.setDefaultQueryTemplateId(null);
        menu.setEntryParamsJson(null);
    }

    private void validateDefaultUiConfig(Menu menu, String moduleAlias) {
        if (menu.getDefaultUiConfigId() == null || menu.getDefaultUiConfigId().isBlank()) {
            menu.setDefaultUiConfigId(null);
            return;
        }
        if (uiConfigService == null || uiSetService == null) {
            return;
        }
        PlatformUiConfig uiConfig = uiConfigService.requireUiConfig(menu.getDefaultUiConfigId());
        PlatformUiSet uiSet = uiSetService.requireUiSet(uiConfig.getUiSetId());
        if (!Objects.equals(uiSet.getModuleAlias(), moduleAlias)) {
            throw new PlatformException("Menu default UI config must belong to module: " + moduleAlias);
        }
        if (uiSet.getSetType() != uiSetType(menu.getPageMode())) {
            throw new PlatformException("Menu default UI config type must match page mode: " + menu.getPageMode());
        }
        if (!Boolean.TRUE.equals(uiSet.getEnabled()) || !Boolean.TRUE.equals(uiConfig.getEnabled())) {
            throw new PlatformException("Menu default UI config must be enabled: " + menu.getDefaultUiConfigId());
        }
        if (!Boolean.TRUE.equals(uiConfig.getPublished())) {
            throw new PlatformException("Menu default UI config must be published: " + menu.getDefaultUiConfigId());
        }
        menu.setDefaultUiConfigId(uiConfig.getId());
    }

    private void validateDefaultQueryTemplate(Menu menu, String moduleAlias) {
        if (menu.getDefaultQueryTemplateId() == null || menu.getDefaultQueryTemplateId().isBlank()) {
            menu.setDefaultQueryTemplateId(null);
            return;
        }
        if (queryTemplateService == null) {
            return;
        }
        PlatformQueryTemplate template = queryTemplateService.requireQueryTemplate(menu.getDefaultQueryTemplateId());
        if (!Objects.equals(template.getModuleAlias(), moduleAlias)) {
            throw new PlatformException("Menu default query template must belong to module: " + moduleAlias);
        }
        if (!Boolean.TRUE.equals(template.getEnabled())) {
            throw new PlatformException("Menu default query template must be enabled: " + menu.getDefaultQueryTemplateId());
        }
        if (!Boolean.TRUE.equals(template.getPublished())) {
            throw new PlatformException("Menu default query template must be published: " + menu.getDefaultQueryTemplateId());
        }
        menu.setDefaultQueryTemplateId(template.getId());
    }

    private void validateParentScheme(Menu menu) {
        validateTreePlacementInScope(menu, schemeScope(menu.getSchemeId()),
                "Menu parent must belong to the same scheme");
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new PlatformException(message);
        }
    }

    private void requireBlank(String value, String message) {
        if (value != null && !value.isBlank()) {
            throw new PlatformException(message);
        }
    }

    private void requireBlankEntry(Menu menu, String message) {
        if (menu.getPageMode() != null
                || hasText(menu.getDefaultUiConfigId())
                || hasText(menu.getDefaultQueryTemplateId())
                || hasText(menu.getEntryParamsJson())) {
            throw new PlatformException(message);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private PlatformUiSetType uiSetType(MenuPageMode pageMode) {
        if (pageMode == MenuPageMode.FORM) {
            return PlatformUiSetType.FORM;
        }
        if (pageMode == MenuPageMode.DETAIL) {
            return PlatformUiSetType.DETAIL;
        }
        return PlatformUiSetType.LIST;
    }

    private void validateImmutableScheme(Menu menu) {
        Menu existing = selectIncludingDeleted(menu.getId());
        rejectChanged(existing, menu, "Menu scheme", Menu::getSchemeId);
    }

    private Criteria schemeScope(String schemeId) {
        return Criteria.of().eq("schemeId", schemeId);
    }

    private boolean isSystemScheme(String schemeId) {
        try (TenantContext.Scope ignored = TenantContext.bypassTenantFilter("resolve menu scheme scope")) {
            MenuScheme scheme = schemeService.select(schemeId);
            return scheme != null && scheme.getScopeType() == MenuScopeType.SYSTEM;
        }
    }
}
