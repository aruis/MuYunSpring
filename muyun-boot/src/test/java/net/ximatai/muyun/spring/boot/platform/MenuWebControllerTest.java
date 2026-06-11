package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.web.BearerTokenCurrentUserProvider;
import net.ximatai.muyun.spring.boot.web.CurrentUserWebFilter;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuScheme;
import net.ximatai.muyun.spring.platform.menu.MenuSchemeService;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import net.ximatai.muyun.spring.platform.menu.MenuScopeType;
import net.ximatai.muyun.spring.platform.menu.MenuType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MenuWebControllerTest {
    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
        TenantContext.clear();
    }

    @Test
    void shouldExposeCurrentUserVisibleMenuTreeWithoutSchemeInput() throws Exception {
        MenuService menuService = mock(MenuService.class);
        MenuWebController controller = new MenuWebController(menuService);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        Menu root = menu("root-1", "scheme-1", "业务中心", null, MenuType.GROUP);
        Menu child = menu("menu-1", "scheme-1", "客户", "crm.customer", MenuType.MODULE);
        when(menuService.currentUserVisibleRootMenus()).thenReturn(List.of(root));
        when(menuService.visibleChildren("scheme-1", "root-1")).thenReturn(List.of(child));
        when(menuService.visibleChildren("scheme-1", "menu-1")).thenReturn(List.of());

        mvc.perform(get("/platform.menu/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].record.id").value("root-1"))
                .andExpect(jsonPath("$.records[0].children[0].record.moduleAlias").value("crm.customer"));
    }

    @Test
    void shouldResolveCurrentUserFromBearerTokenBeforeReturningMineMenuTree() throws Exception {
        MenuService menuService = mock(MenuService.class);
        UserSessionService sessionService = mock(UserSessionService.class);
        MenuWebController controller = new MenuWebController(menuService);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .addFilters(new CurrentUserWebFilter(new BearerTokenCurrentUserProvider(sessionService)))
                .build();
        CurrentUser currentUser = CurrentUser.tenantUser("user-1", "alice", "tenant-a", "dept-1");
        Menu root = menu("root-1", "scheme-1", "业务中心", null, MenuType.GROUP);
        Menu child = menu("menu-1", "scheme-1", "客户", "crm.customer", MenuType.MODULE);
        when(sessionService.currentUser("token-1")).thenReturn(Optional.of(currentUser));
        when(menuService.currentUserVisibleRootMenus()).thenAnswer(invocation -> {
            assertThat(CurrentUserContext.currentUser()).contains(currentUser);
            assertThat(TenantContext.currentTenantId()).contains("tenant-a");
            return List.of(root);
        });
        when(menuService.visibleChildren("scheme-1", "root-1")).thenAnswer(invocation -> {
            assertThat(CurrentUserContext.currentUser()).contains(currentUser);
            assertThat(TenantContext.currentTenantId()).contains("tenant-a");
            return List.of(child);
        });
        when(menuService.visibleChildren("scheme-1", "menu-1")).thenReturn(List.of());

        mvc.perform(get("/platform.menu/mine")
                        .header("Authorization", "Bearer token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].record.id").value("root-1"))
                .andExpect(jsonPath("$.records[0].children[0].record.moduleAlias").value("crm.customer"));

        verify(sessionService).currentUser("token-1");
    }

    @Test
    void shouldExposeMenuSchemeMaintenance() throws Exception {
        TenantContext.setTenantId("tenant-a");
        MenuSchemeService schemeService = mock(MenuSchemeService.class);
        MenuSchemeWebController controller = new MenuSchemeWebController();
        ReflectionTestUtils.setField(controller, "service", schemeService);
        MenuScheme scheme = scheme("scheme-1", "default");
        when(schemeService.pageQuery(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(scheme), 1, PageRequest.of(1, 20)));
        when(schemeService.insert(any(MenuScheme.class))).thenReturn("scheme-1");
        when(schemeService.select("scheme-1")).thenReturn(scheme);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/platform.menu_scheme/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditions":[{"fieldName":"alias","values":["default"]}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].id").value("scheme-1"))
                .andExpect(jsonPath("$.records[0].alias").value("default"));
        mvc.perform(post("/platform.menu_scheme/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"alias":"default","scopeType":"TENANT","title":"Default"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("scheme-1"));

        ArgumentCaptor<MenuScheme> captor = ArgumentCaptor.forClass(MenuScheme.class);
        verify(schemeService).insert(captor.capture());
        assertThat(captor.getValue().getAlias()).isEqualTo("default");
    }

    @Test
    void shouldExposeSchemeScopedMenuMaintenanceAndTree() throws Exception {
        TenantContext.setTenantId("tenant-a");
        MenuService menuService = mock(MenuService.class);
        MenuManagementWebController controller = new MenuManagementWebController();
        ReflectionTestUtils.setField(controller, "service", menuService);
        Menu root = menu("root-1", "scheme-1", "业务中心", null, MenuType.GROUP);
        Menu child = menu("menu-1", "scheme-1", "客户", "crm.customer", MenuType.MODULE);
        Menu inserted = menu("menu-2", "scheme-1", "订单", "crm.order", MenuType.MODULE);
        when(menuService.rootMenus("scheme-1")).thenReturn(List.of(root));
        when(menuService.children("scheme-1", "root-1")).thenReturn(List.of(child));
        when(menuService.children("scheme-1", "menu-1")).thenReturn(List.of());
        when(menuService.insert(any(Menu.class))).thenReturn("menu-2");
        when(menuService.select("menu-2")).thenReturn(inserted);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(get("/platform.menu-scheme/scheme-1/menus/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].record.id").value("root-1"))
                .andExpect(jsonPath("$.records[0].children[0].record.id").value("menu-1"));
        mvc.perform(post("/platform.menu-scheme/scheme-1/menus/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"schemeId":"other-scheme","parentId":"root-1","title":"订单","menuType":"MODULE","moduleAlias":"crm.order"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.schemeId").value("scheme-1"));

        ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
        verify(menuService).insert(captor.capture());
        assertThat(captor.getValue().getSchemeId()).isEqualTo("scheme-1");
    }

    @Test
    void shouldRejectCrossSchemeMenuUpdate() {
        MenuService menuService = mock(MenuService.class);
        MenuManagementWebController controller = new MenuManagementWebController();
        ReflectionTestUtils.setField(controller, "service", menuService);
        when(menuService.select("menu-1")).thenReturn(menu("menu-1", "other-scheme", "客户", "crm.customer", MenuType.MODULE));

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setAttribute(org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                java.util.Map.of("schemeId", "scheme-1"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.update(request, "menu-1", new Menu()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("menu does not belong to scheme");
    }

    private Menu menu(String id, String schemeId, String title, String moduleAlias, MenuType type) {
        Menu menu = new Menu();
        menu.setId(id);
        menu.setSchemeId(schemeId);
        menu.setTitle(title);
        menu.setModuleAlias(moduleAlias);
        menu.setMenuType(type);
        menu.setEnabled(Boolean.TRUE);
        return menu;
    }

    private MenuScheme scheme(String id, String alias) {
        MenuScheme scheme = new MenuScheme();
        scheme.setId(id);
        scheme.setAlias(alias);
        scheme.setTitle(alias);
        scheme.setScopeType(MenuScopeType.TENANT);
        scheme.setScopeId("tenant-a");
        scheme.setEnabled(Boolean.TRUE);
        return scheme;
    }
}
