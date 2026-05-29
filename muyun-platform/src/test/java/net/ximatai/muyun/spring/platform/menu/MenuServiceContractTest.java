package net.ximatai.muyun.spring.platform.menu;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MenuServiceContractTest {
    private final TestMemoryDao<MenuScheme> schemeDao = new TestMemoryDao<>();
    private final TestMemoryDao<Menu> menuDao = new TestMemoryDao<>();
    private final TestMemoryDao<PlatformModule> moduleDao = new TestMemoryDao<>();
    private final PlatformModuleService moduleService = new PlatformModuleService(moduleDao);
    private final MenuSchemeService schemeService = new MenuSchemeService(schemeDao);
    private final MenuService menuService = new MenuService(menuDao, schemeService, moduleService);

    @BeforeEach
    void setUpModules() {
        try (TenantContext.Scope ignored = TenantContext.system()) {
            insertModule("crm.customer");
            insertModule("crm.contract");
        }
    }

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
                    .isInstanceOf(PlatformException.class)
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
    void shouldRequireSystemContextForSystemScheme() {
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            assertThatThrownBy(() -> schemeService.insert(scheme("admin_default", MenuScopeType.SYSTEM, null)))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("system context");
        }
    }

    @Test
    void shouldRejectSchemeIdentityChanges() {
        String schemeId;
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            schemeId = schemeService.insert(scheme("default", MenuScopeType.TENANT, null));
            MenuScheme changedAlias = scheme("mobile", MenuScopeType.TENANT, "tenant-a");
            changedAlias.setId(schemeId);
            changedAlias.setVersion(0);
            assertThatThrownBy(() -> schemeService.update(changedAlias))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("identity");

            MenuScheme changedScope = scheme("default", MenuScopeType.ORGANIZATION, "org-a");
            changedScope.setId(schemeId);
            changedScope.setTenantId("tenant-a");
            changedScope.setVersion(0);
            assertThatThrownBy(() -> schemeService.update(changedScope))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("identity");
        }
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
                    .isInstanceOf(PlatformException.class)
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
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("same scheme");
            assertThatThrownBy(() -> menuService.insert(moduleMenu(firstSchemeId, "错误模块", TreeAbility.ROOT_ID, "customer")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("moduleAlias");
            assertThatThrownBy(() -> menuService.insert(moduleMenu(firstSchemeId, "不存在模块", TreeAbility.ROOT_ID, "crm.unknown")))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("existing module");
            Menu invalidGroup = groupMenu(firstSchemeId, "错误分组", TreeAbility.ROOT_ID);
            invalidGroup.setModuleAlias("crm.customer");
            assertThatThrownBy(() -> menuService.insert(invalidGroup))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("GROUP");
            Menu moduleWithRoute = moduleMenu(firstSchemeId, "错误模块路由", TreeAbility.ROOT_ID, "crm.customer");
            moduleWithRoute.setRoute("/customer");
            assertThatThrownBy(() -> menuService.insert(moduleWithRoute))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("MODULE");
        }
    }

    @Test
    void shouldRejectMenuSchemeChange() {
        String firstSchemeId;
        String secondSchemeId;
        String menuId;
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            firstSchemeId = schemeService.insert(scheme("default", MenuScopeType.TENANT, null));
            secondSchemeId = schemeService.insert(scheme("mobile", MenuScopeType.TENANT, null));
            menuId = menuService.insert(moduleMenu(firstSchemeId, "客户", TreeAbility.ROOT_ID, "crm.customer"));
            Menu moving = moduleMenu(secondSchemeId, "客户", TreeAbility.ROOT_ID, "crm.customer");
            moving.setId(menuId);
            moving.setVersion(0);

            assertThatThrownBy(() -> menuService.update(moving))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("scheme");
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

    private void insertModule(String alias) {
        PlatformModule module = new PlatformModule();
        module.setApplicationAlias(alias.substring(0, alias.indexOf('.')));
        module.setAlias(alias);
        module.setTitle(alias);
        moduleService.insert(module);
    }
}
