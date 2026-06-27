package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.platform.menu.MenuScheme;
import net.ximatai.muyun.spring.platform.menu.MenuSchemeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@PlatformStaticModule(application = "platform", alias = MenuSchemeService.MODULE_ALIAS, title = "平台菜单方案")
@PlatformMenu(parent = PlatformMenuGroups.CONFIG, title = "菜单方案", order = 40)
@RequestMapping("/platform.menu_scheme")
public class MenuSchemeWebController extends net.ximatai.muyun.spring.boot.web.WebSupport<MenuSchemeService>
        implements CrudWeb<MenuScheme, MenuSchemeService>,
        EnableWeb<MenuScheme, MenuSchemeService>,
        SortWeb<MenuScheme, MenuSchemeService> {
}
