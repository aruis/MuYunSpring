package net.ximatai.muyun.spring.platform.menu;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.platform.MenuVisibilityPolicyService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import net.ximatai.muyun.spring.platform.ui.PlatformQueryTemplate;
import net.ximatai.muyun.spring.platform.ui.PlatformQueryTemplateService;
import net.ximatai.muyun.spring.platform.ui.PlatformUiConfig;
import net.ximatai.muyun.spring.platform.ui.PlatformUiConfigService;
import net.ximatai.muyun.spring.platform.ui.PlatformUiSet;
import net.ximatai.muyun.spring.platform.ui.PlatformUiSetService;
import net.ximatai.muyun.spring.platform.ui.PlatformUiSetType;
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
        TreeAbility<Menu> {
    public static final String MODULE_ALIAS = "platform.menu";

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
        return TreeAbility.super.children(schemeScope(schemeId), parentId);
    }

    public List<Menu> visibleRootMenus(String schemeId) {
        return visibleChildren(schemeId, TreeAbility.ROOT_ID, new LinkedHashSet<>());
    }

    public List<Menu> currentUserVisibleRootMenus() {
        CurrentUser user = CurrentUserContext.currentUser()
                .orElseThrow(() -> new PlatformException("current user is required"));
        MenuScheme scheme = schemeService.resolveCurrentUserScheme(user);
        return visibleRootMenus(scheme.getId());
    }

    public Menu currentUserVisibleMenu(String menuId) {
        if (menuId == null || menuId.isBlank()) {
            throw new PlatformException("menuId is required");
        }
        CurrentUser user = CurrentUserContext.currentUser()
                .orElseThrow(() -> new PlatformException("current user is required"));
        MenuScheme scheme = schemeService.resolveCurrentUserScheme(user);
        return findVisibleMenu(scheme.getId(), TreeAbility.ROOT_ID, menuId, new LinkedHashSet<>());
    }

    public Menu currentUserVisibleModuleMenu(String moduleAlias) {
        String validAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        CurrentUser user = CurrentUserContext.currentUser()
                .orElseThrow(() -> new PlatformException("current user is required"));
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
            if (type == MenuType.MODULE) {
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
            if (menu.getMenuType() == MenuType.MODULE && Objects.equals(menu.getModuleAlias(), moduleAlias)) {
                return menu;
            }
            Menu found = findVisibleModuleMenu(schemeId, menu.getId(), moduleAlias, visiting);
            if (found != null) {
                return found;
            }
        }
        return null;
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
                requireBlank(menu.getModuleAlias(), "GROUP menu cannot have moduleAlias");
                requireBlank(menu.getRoute(), "GROUP menu cannot have route");
                requireBlank(menu.getExternalUrl(), "GROUP menu cannot have externalUrl");
                requireBlankEntry(menu, "GROUP menu cannot have low-code entry config");
            }
            case MODULE -> {
                String moduleAlias = PlatformNameRules.requireModuleAlias(menu.getModuleAlias());
                if (moduleService.resolveVisibleModule(moduleAlias) == null) {
                    throw new PlatformException("MODULE menu requires existing module: " + moduleAlias);
                }
                menu.setModuleAlias(moduleAlias);
                requireBlank(menu.getRoute(), "MODULE menu cannot have route");
                requireBlank(menu.getExternalUrl(), "MODULE menu cannot have externalUrl");
                normalizeModuleEntry(menu, moduleAlias);
            }
            case ROUTE -> {
                requireText(menu.getRoute(), "ROUTE menu requires route");
                requireBlank(menu.getModuleAlias(), "ROUTE menu cannot have moduleAlias");
                requireBlank(menu.getExternalUrl(), "ROUTE menu cannot have externalUrl");
                requireBlankEntry(menu, "ROUTE menu cannot have low-code entry config");
            }
            case LINK -> {
                requireText(menu.getExternalUrl(), "LINK menu requires externalUrl");
                requireBlank(menu.getModuleAlias(), "LINK menu cannot have moduleAlias");
                requireBlank(menu.getRoute(), "LINK menu cannot have route");
                requireBlankEntry(menu, "LINK menu cannot have low-code entry config");
            }
        }
    }

    private void normalizeModuleEntry(Menu menu, String moduleAlias) {
        if (menu.getPageMode() == null) {
            menu.setPageMode(MenuPageMode.LIST);
        }
        validateDefaultUiConfig(menu, moduleAlias);
        validateDefaultQueryTemplate(menu, moduleAlias);
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
}
