package net.ximatai.muyun.spring.platform.menu;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.AbilityException;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.util.PlatformAliasRules;
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

    public MenuService(BaseDao<Menu, String> menuDao, MenuSchemeService schemeService) {
        super(MODULE_ALIAS, Menu.class, menuDao);
        this.schemeService = schemeService;
    }

    @Override
    public void beforeInsert(Menu menu) {
        normalizeAndValidate(menu);
    }

    @Override
    public void beforeUpdate(Menu menu) {
        normalizeAndValidate(menu);
    }

    @Override
    public Criteria sortScope(Menu menu) {
        return Criteria.of()
                .eq("schemeId", menu.getSchemeId())
                .eq(PlatformAbilityFields.TREE_PARENT_FIELD, menu.getParentId());
    }

    @Override
    public void validateSortScope(Menu left, Menu right) {
        if (!Objects.equals(left.getSchemeId(), right.getSchemeId())) {
            throw new AbilityException("Menu sort can only move records within the same scheme");
        }
        TreeAbility.super.validateSortScope(left, right);
    }

    @Override
    public List<Menu> children(String parentId) {
        if (TreeAbility.ROOT_ID.equals(parentId)) {
            throw new AbilityException("Use rootMenus(schemeId) to resolve scheme-scoped root menus");
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
        if (!TreeAbility.ROOT_ID.equals(parentId)) {
            Menu parent = selectActiveRaw(parentId);
            if (parent == null || !schemeId.equals(parent.getSchemeId())) {
                return List.of();
            }
        }
        Criteria criteria = activeCriteria(Criteria.of()
                .eq("schemeId", schemeId)
                .eq(PlatformAbilityFields.TREE_PARENT_FIELD, parentId));
        return getDao().query(criteria, new PageRequest(0, Integer.MAX_VALUE), Sort.asc(PlatformAbilityFields.SORT_FIELD));
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
            throw new AbilityException("Menu requires schemeId");
        }
        MenuScheme scheme = schemeService.select(schemeId);
        if (scheme == null) {
            throw new AbilityException("Menu requires existing scheme: " + schemeId);
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
                PlatformAliasRules.requireModuleAlias(menu.getModuleAlias());
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
        String parentId = menu.getParentId();
        if (parentId == null || parentId.isBlank() || TreeAbility.ROOT_ID.equals(parentId)) {
            return;
        }
        Menu parent = select(parentId);
        if (parent == null) {
            return;
        }
        if (!menu.getSchemeId().equals(parent.getSchemeId())) {
            throw new AbilityException("Menu parent must belong to the same scheme");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new AbilityException(message);
        }
    }

    private void requireBlank(String value, String message) {
        if (value != null && !value.isBlank()) {
            throw new AbilityException(message);
        }
    }
}
