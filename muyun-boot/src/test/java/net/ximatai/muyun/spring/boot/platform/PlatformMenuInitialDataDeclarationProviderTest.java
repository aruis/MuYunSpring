package net.ximatai.muyun.spring.boot.platform;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataExecutor;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuOpenMode;
import net.ximatai.muyun.spring.platform.menu.MenuPageMode;
import net.ximatai.muyun.spring.platform.menu.MenuScheme;
import net.ximatai.muyun.spring.platform.menu.MenuSchemeService;
import net.ximatai.muyun.spring.platform.menu.MenuScopeType;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import net.ximatai.muyun.spring.platform.module.ModuleEntryType;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformMenuInitialDataDeclarationProviderTest {
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

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldCreateModuleMenuDeclarationFromCdiBeanAnnotations() {
        Bean bean = mock(Bean.class);
        BeanManager beanManager = mock(BeanManager.class);
        CreationalContext<RouteModuleWeb> creationalContext = mock(CreationalContext.class);
        when(bean.getBeanClass()).thenReturn(RouteModuleWeb.class);
        when(beanManager.getBeans(Object.class, Any.Literal.INSTANCE)).thenReturn(Set.of(bean));
        when(beanManager.createCreationalContext(bean)).thenReturn(creationalContext);
        when(beanManager.getReference(eq(bean), eq(RouteModuleWeb.class), any(CreationalContext.class)))
                .thenReturn(new RouteModuleWeb());
        PlatformMenuInitialDataDeclarationProvider provider =
                new PlatformMenuInitialDataDeclarationProvider(menuService, beanManager);
        InitialDataExecutor executor = new InitialDataExecutor(List.of(), List.of(provider));
        seedBaseline();

        executor.initializeAll();

        Menu menu = menuService.select("platform.menu.module.platform.route");
        assertThat(menu).satisfies(value -> {
            assertThat(value.getSchemeId()).isEqualTo(MenuSchemeService.ADMIN_SCHEME_ID);
            assertThat(value.getParentId()).isEqualTo(PlatformMenuGroups.CONFIG);
            assertThat(value.getTitle()).isEqualTo("路由模块");
            assertThat(value.getModuleAlias()).isEqualTo("platform.route");
            assertThat(value.getRoute()).isEqualTo("/platform/routes");
            assertThat(value.getOpenMode()).isEqualTo(MenuOpenMode.TAB);
            assertThat(value.getPageMode()).isNull();
            assertThat(value.getSortOrder()).isEqualTo(30);
        });
    }

    private void seedBaseline() {
        try (TenantContext.Scope ignored = TenantContext.system("seed menu initial data test")) {
            MenuScheme scheme = new MenuScheme();
            scheme.setId(MenuSchemeService.ADMIN_SCHEME_ID);
            scheme.setAlias(MenuSchemeService.ADMIN_SCHEME_ALIAS);
            scheme.setScopeType(MenuScopeType.SYSTEM);
            scheme.setScopeId(MenuSchemeService.SYSTEM_SCOPE_ID);
            scheme.setTitle("平台超管");
            scheme.setEnabled(Boolean.TRUE);
            schemeService.insert(scheme);

            PlatformModule module = new PlatformModule();
            module.setAlias("platform.route");
            module.setApplicationAlias("platform");
            module.setTitle("路由模块");
            module.setModuleKind(ModuleKind.STATIC);
            module.setEntryType(ModuleEntryType.ROUTE);
            module.setEntryRoute("/platform/routes");
            module.setEnabled(Boolean.TRUE);
            moduleService.insert(module);

            Menu parent = new Menu();
            parent.setId(PlatformMenuGroups.CONFIG);
            parent.setSchemeId(MenuSchemeService.ADMIN_SCHEME_ID);
            parent.setTitle("平台配置");
            parent.setEnabled(Boolean.TRUE);
            parent.setSortOrder(10);
            menuService.insert(parent);
        }
    }

    @PlatformStaticModule(
            application = "platform",
            alias = "platform.route",
            title = "路由模块",
            route = "/platform/routes"
    )
    @PlatformMenu(parent = PlatformMenuGroups.CONFIG, order = 30)
    static class RouteModuleWeb {
    }
}
