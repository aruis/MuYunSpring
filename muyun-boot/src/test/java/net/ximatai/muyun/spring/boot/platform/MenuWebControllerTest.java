package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import net.ximatai.muyun.spring.platform.menu.MenuType;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MenuWebControllerTest {
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
