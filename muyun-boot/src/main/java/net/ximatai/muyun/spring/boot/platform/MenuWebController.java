package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebTreeNode;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/platform.menu")
public class MenuWebController {
    private final MenuService menuService;

    public MenuWebController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/mine")
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
