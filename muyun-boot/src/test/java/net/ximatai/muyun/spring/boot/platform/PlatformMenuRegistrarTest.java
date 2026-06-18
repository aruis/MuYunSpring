package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuPageMode;
import net.ximatai.muyun.spring.platform.menu.MenuScheme;
import net.ximatai.muyun.spring.platform.menu.MenuSchemeService;
import net.ximatai.muyun.spring.platform.menu.MenuScopeType;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import net.ximatai.muyun.spring.platform.menu.MenuType;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataAbility;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataConflictException;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PlatformMenuRegistrarTest {
    private final TestMemoryDao<MenuScheme> schemeDao = new TestMemoryDao<>();
    private final TestMemoryDao<Menu> menuDao = new TestMemoryDao<>();
    private final TestMemoryDao<PlatformModule> moduleDao = new TestMemoryDao<>();
    private final MenuSchemeService schemeService = new MenuSchemeService(schemeDao);
    private final PlatformModuleService moduleService = new PlatformModuleService(moduleDao);
    private final MenuService menuService = new MenuService(
            menuDao,
            schemeService,
            moduleService,
            Optional.of((moduleAlias, currentUser) -> true)
    );

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
        TenantContext.clear();
    }

    @Test
    void shouldRegisterPlatformAdminSchemeGroupsAndModuleMenus() {
        try (GenericApplicationContext context = context(PlatformModuleWeb.class, IamRoleWeb.class, HiddenModuleWeb.class)) {
            registerStaticModules(context);
            initializePlatformMenus(context);

            MenuScheme scheme = schemeService.select(PlatformAdminMenuInitialDataContribution.ADMIN_SCHEME_ID);
            assertThat(scheme).satisfies(value -> {
                assertThat(value.getAlias()).isEqualTo(PlatformAdminMenuInitialDataContribution.ADMIN_SCHEME_ALIAS);
                assertThat(value.getScopeType()).isEqualTo(MenuScopeType.SYSTEM);
                assertThat(value.getScopeId()).isEqualTo(MenuSchemeService.SYSTEM_SCOPE_ID);
                assertThat(value.getTitle()).isEqualTo("平台超管");
            });
            assertThat(menuService.rootMenus(scheme.getId()))
                    .extracting(Menu::getId)
                    .containsExactly(
                            PlatformMenuGroups.CONFIG,
                            PlatformMenuGroups.IDENTITY,
                            PlatformMenuGroups.OPS
                    );
            assertThat(menuService.children(scheme.getId(), PlatformMenuGroups.CONFIG))
                    .extracting(Menu::getId)
                    .containsExactly("platform.menu.module.platform.module");
            assertThat(menuService.select("platform.menu.module.platform.module")).satisfies(menu -> {
                assertThat(menu.getMenuType()).isEqualTo(MenuType.MODULE);
                assertThat(menu.getModuleAlias()).isEqualTo("platform.module");
                assertThat(menu.getPageMode()).isEqualTo(MenuPageMode.LIST);
                assertThat(menu.getTitle()).isEqualTo("模块管理");
                assertThat(menu.getSortOrder()).isEqualTo(20);
            });
            assertThat(menuService.children(scheme.getId(), PlatformMenuGroups.IDENTITY))
                    .extracting(Menu::getId)
                    .containsExactly("platform.menu.module.iam.role");
            assertThat(menuService.select("platform.menu.module.platform.hidden")).isNull();
        }
    }

    @Test
    void shouldKeepModuleMenuUnchangedWhenReinitializing() {
        try (GenericApplicationContext context = context(PlatformModuleWeb.class)) {
            registerStaticModules(context);
            initializePlatformMenus(context);
            Menu existing = menuService.select("platform.menu.module.platform.module");
            Integer version = existing.getVersion();

            initializePlatformMenus(context);

            assertThat(menuService.select("platform.menu.module.platform.module")).satisfies(menu -> {
                assertThat(menu.getPageMode()).isEqualTo(MenuPageMode.LIST);
                assertThat(menu.getVersion()).isEqualTo(version);
            });
        }
    }

    @Test
    void shouldPreserveOperatorFieldsWhenRegisteredMenusExist() {
        try (GenericApplicationContext context = context(PlatformModuleWeb.class)) {
            registerStaticModules(context);
            initializePlatformMenus(context);

            Menu existing = menuService.select("platform.menu.module.platform.module");
            existing.setTitle("旧标题");
            existing.setSortOrder(999);
            menuService.update(existing);

            initializePlatformMenus(context);

            assertThat(menuService.rootMenus(PlatformAdminMenuInitialDataContribution.ADMIN_SCHEME_ID)).hasSize(3);
            assertThat(menuService.children(PlatformAdminMenuInitialDataContribution.ADMIN_SCHEME_ID, PlatformMenuGroups.CONFIG))
                    .singleElement()
                    .satisfies(menu -> {
                        assertThat(menu.getTitle()).isEqualTo("旧标题");
                        assertThat(menu.getSortOrder()).isEqualTo(999);
                    });
        }
    }

    @Test
    void shouldRepairManagedMenuStructureDrift() {
        try (GenericApplicationContext context = context(PlatformModuleWeb.class)) {
            registerStaticModules(context);
            initializePlatformMenus(context);

            Menu group = menuDao.findById(PlatformMenuGroups.CONFIG);
            group.setParentId("wrong-parent");
            group.setMenuType(MenuType.ROUTE);
            group.setRoute("/wrong");
            Menu moduleMenu = menuDao.findById("platform.menu.module.platform.module");
            moduleMenu.setMenuType(MenuType.LINK);
            moduleMenu.setExternalUrl("https://example.com");

            initializePlatformMenus(context);

            assertThat(menuService.select(PlatformMenuGroups.CONFIG)).satisfies(repaired -> {
                assertThat(repaired.getSchemeId()).isEqualTo(PlatformAdminMenuInitialDataContribution.ADMIN_SCHEME_ID);
                assertThat(repaired.getParentId()).isEqualTo(net.ximatai.muyun.spring.ability.TreeAbility.ROOT_ID);
                assertThat(repaired.getMenuType()).isEqualTo(MenuType.GROUP);
                assertThat(repaired.getRoute()).isNull();
            });
            assertThat(menuService.select("platform.menu.module.platform.module")).satisfies(repaired -> {
                assertThat(repaired.getSchemeId()).isEqualTo(PlatformAdminMenuInitialDataContribution.ADMIN_SCHEME_ID);
                assertThat(repaired.getParentId()).isEqualTo(PlatformMenuGroups.CONFIG);
                assertThat(repaired.getMenuType()).isEqualTo(MenuType.MODULE);
                assertThat(repaired.getModuleAlias()).isEqualTo("platform.module");
                assertThat(repaired.getExternalUrl()).isNull();
            });
        }
    }

    @Test
    void shouldRejectManagedMenuSchemeDrift() {
        try (GenericApplicationContext context = context(PlatformModuleWeb.class)) {
            registerStaticModules(context);
            initializePlatformMenus(context);

            Menu moduleMenu = menuDao.findById("platform.menu.module.platform.module");
            moduleMenu.setSchemeId("wrong-scheme");

            assertThatThrownBy(() -> initializePlatformMenus(context))
                    .isInstanceOf(InitialDataConflictException.class)
                    .hasMessageContaining("platform.menu.module.platform.module.schemeId");
        }
    }

    @Test
    void shouldRejectSoftDeletedManagedMenu() {
        try (GenericApplicationContext context = context(PlatformModuleWeb.class)) {
            registerStaticModules(context);
            initializePlatformMenus(context);

            Menu moduleMenu = menuDao.findById("platform.menu.module.platform.module");
            moduleMenu.setDeleted(Boolean.TRUE);

            assertThatThrownBy(() -> initializePlatformMenus(context))
                    .isInstanceOf(InitialDataConflictException.class)
                    .hasMessageContaining("soft-deleted: platform.menu.module.platform.module");
        }
    }

    @Test
    void shouldExposeRegisteredPlatformAdminMenusToSystemUser() {
        try (GenericApplicationContext context = context(PlatformModuleWeb.class, IamRoleWeb.class)) {
            registerStaticModules(context);
            initializePlatformMenus(context);

            try (CurrentUserContext.Scope ignored = CurrentUserContext.use(
                    CurrentUser.systemUser("system-user", "System"))) {
                List<Menu> roots = menuService.currentUserVisibleRootMenus();

                assertThat(roots).extracting(Menu::getId)
                        .containsExactly(PlatformMenuGroups.CONFIG, PlatformMenuGroups.IDENTITY);
            }
        }
    }

    @Test
    void shouldRejectPlatformMenuWithoutStaticModule() {
        try (GenericApplicationContext context = context(InvalidMenuWeb.class)) {
            assertThatThrownBy(() -> initializePlatformMenus(context))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("@PlatformMenu requires @PlatformStaticModule");
        }
    }

    private void initializePlatformMenus(GenericApplicationContext context) {
        new InitialDataAbility(List.of(
                new PlatformAdminMenuInitialDataContribution(schemeService, menuService),
                new PlatformMenuRegistrar(menuService, context)
        )).initializeAll();
    }

    private void registerStaticModules(GenericApplicationContext context) {
        new StaticModuleDefinitionRegistrar(
                moduleService,
                mock(PlatformModuleActionService.class),
                List.of(),
                List.of(new StaticModuleDefinitionScanner(context))
        ).registerAll();
    }

    @SafeVarargs
    private GenericApplicationContext context(Class<?>... beanClasses) {
        GenericApplicationContext context = new GenericApplicationContext();
        for (Class<?> beanClass : beanClasses) {
            context.registerBean(beanClass);
        }
        context.refresh();
        return context;
    }

    @RestController
    @PlatformStaticModule(application = "platform", alias = "platform.module", title = "平台模块")
    @PlatformMenu(parent = PlatformMenuGroups.CONFIG, title = "模块管理", order = 20)
    @RequestMapping("/platform.module")
    static class PlatformModuleWeb {
    }

    @RestController
    @PlatformStaticModule(application = "iam", alias = "iam.role", title = "角色管理")
    @PlatformMenu(parent = PlatformMenuGroups.IDENTITY, order = 10)
    @RequestMapping("/iam.role")
    static class IamRoleWeb {
    }

    @RestController
    @PlatformStaticModule(application = "platform", alias = "platform.hidden", title = "隐藏模块")
    @RequestMapping("/platform.hidden")
    static class HiddenModuleWeb {
    }

    @RestController
    @PlatformMenu(parent = PlatformMenuGroups.CONFIG)
    static class InvalidMenuWeb {
    }
}
