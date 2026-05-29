package net.ximatai.muyun.spring.platform.menu;

import net.ximatai.muyun.spring.ability.AbilityException;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MenuServiceContractTest {
    private final TestMemoryDao<MenuScheme> schemeDao = new TestMemoryDao<>();
    private final TestMemoryDao<Menu> menuDao = new TestMemoryDao<>();
    private final MenuSchemeService schemeService = new MenuSchemeService(schemeDao);
    private final MenuService menuService = new MenuService(menuDao, schemeService);

    @Test
    void shouldCreateTenantMenuSchemeWithScopeAliasAndTenantIsolation() {
        String tenantASchemeId;
        String tenantBSchemeId;
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            tenantASchemeId = schemeService.insert(scheme("default", MenuScopeType.TENANT, null));
        }
        try (TenantContext.Scope ignored = TenantContext.use("tenant-b")) {
            tenantBSchemeId = schemeService.insert(scheme("default", MenuScopeType.TENANT, null));
        }

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            assertThat(schemeService.select(tenantASchemeId)).isNotNull();
            assertThat(schemeService.select(tenantBSchemeId)).isNull();
            assertThat(schemeService.select(tenantASchemeId).getScopeId()).isEqualTo("tenant-a");
        }
    }

    @Test
    void shouldRejectDuplicateSchemeAliasWithinSameScope() {
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            schemeService.insert(scheme("default", MenuScopeType.TENANT, null));

            assertThatThrownBy(() -> schemeService.insert(scheme("default", MenuScopeType.TENANT, null)))
                    .isInstanceOf(AbilityException.class)
                    .hasMessageContaining("unique");
        }
    }

    @Test
    void shouldCreateSystemSchemeWithoutTenant() {
        String schemeId;
        try (TenantContext.Scope ignored = TenantContext.system()) {
            schemeId = schemeService.insert(scheme("admin_default", MenuScopeType.SYSTEM, null));
        }

        MenuScheme saved = schemeService.select(schemeId);
        assertThat(saved.getTenantId()).isNull();
        assertThat(saved.getScopeId()).isEqualTo(MenuSchemeService.SYSTEM_SCOPE_ID);
    }

    @Test
    void shouldCreateSchemeScopedMenuTreeAndRejectUnscopedRootLookup() {
        String schemeId;
        String rootId;
        String childId;
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            schemeId = schemeService.insert(scheme("default", MenuScopeType.TENANT, null));
            rootId = menuService.insert(groupMenu(schemeId, "客户中心", TreeAbility.ROOT_ID));
            childId = menuService.insert(moduleMenu(schemeId, "客户", rootId, "crm.customer"));
        }

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            assertThat(menuService.rootMenus(schemeId)).extracting(Menu::getId).containsExactly(rootId);
            assertThat(menuService.children(schemeId, rootId)).extracting(Menu::getId).containsExactly(childId);
            assertThatThrownBy(() -> menuService.children(TreeAbility.ROOT_ID))
                    .isInstanceOf(AbilityException.class)
                    .hasMessageContaining("rootMenus");
        }
    }

    @Test
    void shouldIsolateMenusBetweenTenants() {
        String tenantASchemeId;
        String tenantBSchemeId;
        String tenantAMenuId;
        String tenantBMenuId;
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            tenantASchemeId = schemeService.insert(scheme("default", MenuScopeType.TENANT, null));
            tenantAMenuId = menuService.insert(moduleMenu(tenantASchemeId, "客户", TreeAbility.ROOT_ID, "crm.customer"));
        }
        try (TenantContext.Scope ignored = TenantContext.use("tenant-b")) {
            tenantBSchemeId = schemeService.insert(scheme("default", MenuScopeType.TENANT, null));
            tenantBMenuId = menuService.insert(moduleMenu(tenantBSchemeId, "客户", TreeAbility.ROOT_ID, "crm.customer"));
        }

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            assertThat(menuService.rootMenus(tenantASchemeId)).extracting(Menu::getId).containsExactly(tenantAMenuId);
            assertThat(menuService.rootMenus(tenantBSchemeId)).isEmpty();
            assertThat(menuService.select(tenantBMenuId)).isNull();
        }
    }

    @Test
    void shouldAllowSameModuleMountedByDifferentMenus() {
        String firstSchemeId;
        String secondSchemeId;
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            firstSchemeId = schemeService.insert(scheme("default", MenuScopeType.TENANT, null));
            secondSchemeId = schemeService.insert(scheme("mobile", MenuScopeType.TENANT, null));
            menuService.insert(moduleMenu(firstSchemeId, "客户", TreeAbility.ROOT_ID, "crm.customer"));
            menuService.insert(moduleMenu(secondSchemeId, "客户移动端", TreeAbility.ROOT_ID, "crm.customer"));
        }

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            assertThat(menuService.rootMenus(firstSchemeId)).extracting(Menu::getModuleAlias).containsExactly("crm.customer");
            assertThat(menuService.rootMenus(secondSchemeId)).extracting(Menu::getModuleAlias).containsExactly("crm.customer");
        }
    }

    @Test
    void shouldRejectParentAcrossSchemesAndInvalidTargets() {
        String firstSchemeId;
        String secondSchemeId;
        String firstRootId;
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            firstSchemeId = schemeService.insert(scheme("default", MenuScopeType.TENANT, null));
            secondSchemeId = schemeService.insert(scheme("mobile", MenuScopeType.TENANT, null));
            firstRootId = menuService.insert(groupMenu(firstSchemeId, "客户中心", TreeAbility.ROOT_ID));

            assertThatThrownBy(() -> menuService.insert(moduleMenu(secondSchemeId, "错误节点", firstRootId, "crm.customer")))
                    .isInstanceOf(AbilityException.class)
                    .hasMessageContaining("same scheme");
            assertThatThrownBy(() -> menuService.insert(moduleMenu(firstSchemeId, "错误模块", TreeAbility.ROOT_ID, "customer")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("moduleAlias");
            Menu invalidGroup = groupMenu(firstSchemeId, "错误分组", TreeAbility.ROOT_ID);
            invalidGroup.setModuleAlias("crm.customer");
            assertThatThrownBy(() -> menuService.insert(invalidGroup))
                    .isInstanceOf(AbilityException.class)
                    .hasMessageContaining("GROUP");
        }
    }

    @Test
    void shouldReorderMenusWithinSameSchemeAndParent() {
        String schemeId;
        String firstId;
        String secondId;
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            schemeId = schemeService.insert(scheme("default", MenuScopeType.TENANT, null));
            firstId = menuService.insert(moduleMenu(schemeId, "客户", TreeAbility.ROOT_ID, "crm.customer"));
            secondId = menuService.insert(moduleMenu(schemeId, "合同", TreeAbility.ROOT_ID, "crm.contract"));

            menuService.reorder(List.of(secondId, firstId));
        }

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            assertThat(menuService.rootMenus(schemeId))
                    .extracting(Menu::getId)
                    .containsExactly(secondId, firstId);
        }
    }

    private MenuScheme scheme(String alias, MenuScopeType scopeType, String scopeId) {
        MenuScheme scheme = new MenuScheme();
        scheme.setAlias(alias);
        scheme.setScopeType(scopeType);
        scheme.setScopeId(scopeId);
        scheme.setTitle(alias);
        return scheme;
    }

    private Menu groupMenu(String schemeId, String title, String parentId) {
        Menu menu = new Menu();
        menu.setSchemeId(schemeId);
        menu.setTitle(title);
        menu.setParentId(parentId);
        menu.setMenuType(MenuType.GROUP);
        return menu;
    }

    private Menu moduleMenu(String schemeId, String title, String parentId, String moduleAlias) {
        Menu menu = new Menu();
        menu.setSchemeId(schemeId);
        menu.setTitle(title);
        menu.setParentId(parentId);
        menu.setMenuType(MenuType.MODULE);
        menu.setModuleAlias(moduleAlias);
        return menu;
    }
}
