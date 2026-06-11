package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.platform.menu.MenuScheme;
import net.ximatai.muyun.spring.platform.menu.MenuSchemeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@PlatformStaticModule(application = "platform", alias = MenuSchemeService.MODULE_ALIAS, title = "平台菜单方案")
@RequestMapping("/platform.menu_scheme")
public class MenuSchemeWebController extends net.ximatai.muyun.spring.boot.web.WebSupport<MenuSchemeService>
        implements CrudWeb<MenuScheme, MenuSchemeService>,
        EnableWeb<MenuScheme, MenuSchemeService>,
        SortWeb<MenuScheme, MenuSchemeService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "id", "alias", "scopeType", "scopeId", "enabled", "sortOrder", "createdAt", "updatedAt");

    @Override
    public Criteria queryCriteria(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.criteria(request, QUERY_FIELDS, webScopeName());
    }

    @Override
    public Sort[] querySorts(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.sorts(request, QUERY_FIELDS, Sort.asc("sortOrder"), Sort.asc("alias"));
    }
}
