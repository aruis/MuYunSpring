package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.boot.web.NestedEnabledTreeCrudWebSupport;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@PlatformStaticModule(application = net.ximatai.muyun.spring.boot.platform.PlatformApplication.class, alias = MenuService.MODULE_ALIAS, title = "平台菜单",
        webScope = PlatformStaticModule.WebScope.CUSTOM)
@RequestMapping("/platform.menu-scheme/{schemeId}/menus")
public class MenuManagementWebController extends NestedEnabledTreeCrudWebSupport<Menu, MenuService> {

    @Override
    protected Criteria treeScopeCriteria(HttpServletRequest request) {
        return Criteria.of().eq("schemeId", schemeId(request));
    }

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        criteria.eq("schemeId", schemeId(request));
    }

    @Override
    protected void bindScope(Menu record, HttpServletRequest request) {
        record.setSchemeId(schemeId(request));
    }

    @Override
    protected boolean inScope(Menu record, HttpServletRequest request) {
        return Objects.equals(record.getSchemeId(), schemeId(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "menu does not belong to scheme: " + schemeId(request) + "." + id;
    }

    private String schemeId(HttpServletRequest request) {
        String value = pathVariable(request, "schemeId");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("schemeId is required");
        }
        return value;
    }
}
