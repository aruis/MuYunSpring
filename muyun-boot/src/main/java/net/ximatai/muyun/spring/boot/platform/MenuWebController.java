package net.ximatai.muyun.spring.boot.platform;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebTreeNode;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuService;

import java.util.List;

@ApplicationScoped
@Path("/platform.menu")
public class MenuWebController {
    private final MenuService menuService;

    public MenuWebController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GET
    @Path("/mine")
    public WebListResponse<WebTreeNode<Menu>> mine() {
        return new WebListResponse<>(menuService.currentUserVisibleRootMenus().stream()
                .map(this::node)
                .toList());
    }

    private WebTreeNode<Menu> node(Menu menu) {
        List<WebTreeNode<Menu>> children = menuService.visibleChildren(menu.getSchemeId(), menu.getId())
                .stream()
                .map(this::node)
                .toList();
        return new WebTreeNode<>(menu, children);
    }
}
