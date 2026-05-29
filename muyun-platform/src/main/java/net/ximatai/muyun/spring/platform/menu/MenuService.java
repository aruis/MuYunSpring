package net.ximatai.muyun.spring.platform.menu;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class MenuService extends AbstractAbilityService<Menu> implements
        SoftDeleteAbility<Menu>,
        EnableAbility<Menu>,
        TreeAbility<Menu> {
    public static final String MODULE_ALIAS = "platform.menu";

    private final MenuSchemeService schemeService;
    private final PlatformModuleService moduleService;

    public MenuService(BaseDao<Menu, String> menuDao, MenuSchemeService schemeService, PlatformModuleService moduleService) {
        super(MODULE_ALIAS, Menu.class, menuDao);
        this.schemeService = schemeService;
        this.moduleService = moduleService;
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
            }
            case MODULE -> {
                String moduleAlias = PlatformNameRules.requireModuleAlias(menu.getModuleAlias());
                if (moduleService.resolveVisibleModule(moduleAlias) == null) {
                    throw new PlatformException("MODULE menu requires existing module: " + moduleAlias);
                }
                menu.setModuleAlias(moduleAlias);
                requireBlank(menu.getRoute(), "MODULE menu cannot have route");
                requireBlank(menu.getExternalUrl(), "MODULE menu cannot have externalUrl");
            }
            case ROUTE -> {
                requireText(menu.getRoute(), "ROUTE menu requires route");
                requireBlank(menu.getModuleAlias(), "ROUTE menu cannot have moduleAlias");
                requireBlank(menu.getExternalUrl(), "ROUTE menu cannot have externalUrl");
            }
            case LINK -> {
                requireText(menu.getExternalUrl(), "LINK menu requires externalUrl");
                requireBlank(menu.getModuleAlias(), "LINK menu cannot have moduleAlias");
                requireBlank(menu.getRoute(), "LINK menu cannot have route");
            }
        }
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

    private void validateImmutableScheme(Menu menu) {
        Menu existing = selectIncludingDeleted(menu.getId());
        rejectChanged(existing, menu, "Menu scheme", Menu::getSchemeId);
    }

    private Criteria schemeScope(String schemeId) {
        return Criteria.of().eq("schemeId", schemeId);
    }
}
