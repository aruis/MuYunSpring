package net.ximatai.muyun.spring.boot.platform;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.web.NestedCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebPageResponse;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.boot.web.WebRecordResponse;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.boot.web.WebTreeNode;
import net.ximatai.muyun.spring.common.exception.PlatformConfigurationException;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuOpenMode;
import net.ximatai.muyun.spring.platform.menu.MenuScheme;
import net.ximatai.muyun.spring.platform.menu.MenuSchemeService;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import net.ximatai.muyun.spring.platform.menu.MenuScopeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MenuWebControllerTest {
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldDeclareCurrentUserMenuRoute() throws Exception {
        assertThat(MenuWebController.class.getAnnotation(Path.class).value()).isEqualTo("/platform.menu");
        Method method = MenuWebController.class.getMethod("mine");
        assertThat(method.getAnnotation(GET.class)).isNotNull();
        assertThat(method.getAnnotation(Path.class).value()).isEqualTo("/mine");
    }

    @Test
    void shouldExposeCurrentUserVisibleMenuTreeWithoutSchemeInput() {
        MenuService menuService = mock(MenuService.class);
        MenuWebController controller = new MenuWebController(menuService);
        Menu root = menu("root-1", "scheme-1", "业务中心", null);
        Menu child = menu("menu-1", "scheme-1", "客户", "crm.customer");
        when(menuService.currentUserVisibleRootMenus()).thenReturn(List.of(root));
        when(menuService.visibleChildren("scheme-1", "root-1")).thenReturn(List.of(child));
        when(menuService.visibleChildren("scheme-1", "menu-1")).thenReturn(List.of());

        WebListResponse<WebTreeNode<Menu>> response = controller.mine();

        assertThat(response.records()).singleElement().satisfies(rootNode -> {
            assertThat(rootNode.record().getId()).isEqualTo("root-1");
            assertThat(rootNode.children()).singleElement().satisfies(childNode -> {
                assertThat(childNode.record().getOpenMode()).isEqualTo(MenuOpenMode.TAB);
                assertThat(childNode.record().getModuleAlias()).isEqualTo("crm.customer");
            });
        });
    }

    @Test
    void shouldPropagateConfigurationErrorWhenCurrentUserHasNoMenuScheme() {
        MenuService menuService = mock(MenuService.class);
        MenuWebController controller = new MenuWebController(menuService);
        when(menuService.currentUserVisibleRootMenus())
                .thenThrow(new PlatformConfigurationException("menu scheme is not configured for current user"));

        assertThatThrownBy(controller::mine)
                .isInstanceOf(PlatformConfigurationException.class)
                .hasMessageContaining("menu scheme is not configured for current user");
    }

    @Test
    void shouldExposeMenuSchemeMaintenance() throws Exception {
        TenantContext.setTenantId("tenant-a");
        MenuSchemeService schemeService = mock(MenuSchemeService.class);
        MenuSchemeWebController controller = new MenuSchemeWebController();
        setService(controller, schemeService);
        MenuScheme scheme = scheme("scheme-1", "default");
        when(schemeService.pageQuery(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(scheme), 1, PageRequest.of(1, 20)));
        when(schemeService.insert(any(MenuScheme.class))).thenReturn("scheme-1");
        when(schemeService.select("scheme-1")).thenReturn(scheme);

        WebPageResponse<MenuScheme> page = controller.query(new WebQueryRequest(null, List.of(), null));
        WebRecordResponse<MenuScheme> inserted = controller.insert(scheme(null, "default"));

        assertThat(page.records()).singleElement().satisfies(record -> {
            assertThat(record.getId()).isEqualTo("scheme-1");
            assertThat(record.getAlias()).isEqualTo("default");
        });
        assertThat(inserted.record().getId()).isEqualTo("scheme-1");
        ArgumentCaptor<MenuScheme> captor = ArgumentCaptor.forClass(MenuScheme.class);
        verify(schemeService).insert(captor.capture());
        assertThat(captor.getValue().getAlias()).isEqualTo("default");
    }

    @Test
    void shouldDeclareSchemeScopedMenuRoutes() throws Exception {
        assertThat(MenuManagementWebController.class.getAnnotation(Path.class).value())
                .isEqualTo("/platform.menu-scheme/{schemeId}/menus");
        Method tree = MenuManagementWebController.class.getMethod("tree", HttpServletRequest.class, boolean.class);
        assertThat(tree.getAnnotation(GET.class)).isNotNull();
        assertThat(tree.getAnnotation(Path.class).value()).isEqualTo("/tree");
        assertThat(tree.getParameters()[0].getAnnotation(Context.class)).isNotNull();
        assertThat(tree.getParameters()[1].getAnnotation(QueryParam.class).value()).isEqualTo("flat");
        assertThat(tree.getParameters()[1].getAnnotation(DefaultValue.class).value()).isEqualTo("false");
        assertThat(tree.getAnnotation(ActionEndpoint.class).value()).isEqualTo(PlatformAction.TREE);

        Method childTree = MenuManagementWebController.class.getMethod(
                "tree", HttpServletRequest.class, String.class, boolean.class, boolean.class);
        assertThat(childTree.getAnnotation(Path.class).value()).isEqualTo("/tree/{id}");
        assertThat(childTree.getParameters()[1].getAnnotation(PathParam.class).value()).isEqualTo("id");
        assertThat(childTree.getAnnotation(ActionEndpoint.class).value()).isEqualTo(PlatformAction.TREE);
    }

    @Test
    void shouldExposeSchemeScopedMenuMaintenanceAndTree() throws Exception {
        TenantContext.setTenantId("tenant-a");
        MenuService menuService = mock(MenuService.class);
        MenuManagementWebController controller = new MenuManagementWebController();
        setService(controller, menuService);
        Menu root = menu("root-1", "scheme-1", "业务中心", null);
        Menu child = menu("menu-1", "scheme-1", "客户", "crm.customer");
        Menu inserted = menu("menu-2", "scheme-1", "订单", "crm.order");
        when(menuService.rootMenus("scheme-1")).thenReturn(List.of(root));
        when(menuService.children("scheme-1", "root-1")).thenReturn(List.of(child));
        when(menuService.children("scheme-1", "menu-1")).thenReturn(List.of());
        when(menuService.insert(any(Menu.class))).thenReturn("menu-2");
        when(menuService.select("menu-2")).thenReturn(inserted);

        WebListResponse<?> tree = controller.tree(requestVars("scheme-1"), false);
        WebRecordResponse<Menu> saved = controller.insert(requestVars("scheme-1"),
                menu(null, "other-scheme", "订单", "crm.order"));

        assertThat(tree.records()).singleElement().isInstanceOfSatisfying(WebTreeNode.class, node -> {
            assertThat(((Menu) node.record()).getId()).isEqualTo("root-1");
            assertThat(node.children()).singleElement().isInstanceOfSatisfying(WebTreeNode.class, childNode -> {
                WebTreeNode<?> typedChild = (WebTreeNode<?>) childNode;
                assertThat(((Menu) typedChild.record()).getId()).isEqualTo("menu-1");
            });
        });
        assertThat(saved.record().getSchemeId()).isEqualTo("scheme-1");
        ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
        verify(menuService).insert(captor.capture());
        assertThat(captor.getValue().getSchemeId()).isEqualTo("scheme-1");
    }

    @Test
    void shouldRejectCrossSchemeMenuUpdate() throws Exception {
        MenuService menuService = mock(MenuService.class);
        MenuManagementWebController controller = new MenuManagementWebController();
        setService(controller, menuService);
        when(menuService.select("menu-1")).thenReturn(menu("menu-1", "other-scheme", "客户", "crm.customer"));

        assertThatThrownBy(() -> controller.update(requestVars("scheme-1"), "menu-1", new Menu()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("menu does not belong to scheme");
    }

    private Menu menu(String id, String schemeId, String title, String moduleAlias) {
        Menu menu = new Menu();
        menu.setId(id);
        menu.setSchemeId(schemeId);
        menu.setTitle(title);
        menu.setModuleAlias(moduleAlias);
        if (moduleAlias != null && !moduleAlias.isBlank()) {
            menu.setOpenMode(MenuOpenMode.TAB);
        }
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

    private HttpServletRequest requestVars(String schemeId) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(NestedCrudWebSupport.PATH_VARIABLES_ATTRIBUTE))
                .thenReturn(Map.of("schemeId", schemeId));
        return request;
    }

    private void setService(Object target, Object service) throws ReflectiveOperationException {
        Field field = WebSupport.class.getDeclaredField("service");
        field.setAccessible(true);
        field.set(target, service);
    }
}
