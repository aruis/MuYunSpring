package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.boot.web.BearerTokenCurrentUserProvider;
import net.ximatai.muyun.spring.boot.web.CurrentUserWebFilter;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import net.ximatai.muyun.spring.platform.menu.MenuType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
