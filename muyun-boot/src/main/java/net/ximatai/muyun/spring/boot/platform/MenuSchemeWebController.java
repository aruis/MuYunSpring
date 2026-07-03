package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.platform.menu.MenuScheme;
import net.ximatai.muyun.spring.platform.menu.MenuSchemeService;
import jakarta.ws.rs.Path;
import jakarta.enterprise.context.ApplicationScoped;


@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = MenuSchemeService.MODULE_ALIAS, title = "平台菜单方案",
        route = "/config/menus")
@PlatformMenu(parent = PlatformMenuGroups.CONFIG, title = "菜单方案", order = 40)
@Path("/platform.menu_scheme")
public class MenuSchemeWebController extends net.ximatai.muyun.spring.boot.web.WebSupport<MenuSchemeService>
        implements CrudWeb<MenuScheme, MenuSchemeService>,
        EnableWeb<MenuScheme, MenuSchemeService>,
        SortWeb<MenuScheme, MenuSchemeService> {
}
