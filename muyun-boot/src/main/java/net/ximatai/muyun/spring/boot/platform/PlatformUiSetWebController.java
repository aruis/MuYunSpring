package net.ximatai.muyun.spring.boot.platform;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.platform.ui.PlatformUiSet;
import net.ximatai.muyun.spring.platform.ui.PlatformUiSetService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.Set;

@RestController
@PlatformStaticModule(application = "platform", alias = PlatformUiSetService.MODULE_ALIAS, title = "平台 UI 配置集")
@RequestMapping("/platform.module/{moduleAlias}/ui-sets")
public class PlatformUiSetWebController
        extends NestedEnabledSortableCrudWebSupport<PlatformUiSet, PlatformUiSetService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "alias", "title", "setType", "defaultSet", "enabled");

    @Override
    protected Criteria queryCriteria(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.criteria(request, QUERY_FIELDS, "platform UI set query");
    }

    @Override
    protected Sort[] querySorts(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.sorts(request, QUERY_FIELDS, Sort.asc("sortOrder"), Sort.asc("alias"));
    }

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        criteria.eq("moduleAlias", pathVariable(request, "moduleAlias"));
    }

    @Override
    protected void bindScope(PlatformUiSet record, HttpServletRequest request) {
        record.setModuleAlias(pathVariable(request, "moduleAlias"));
    }

    @Override
    protected boolean inScope(PlatformUiSet record, HttpServletRequest request) {
        return Objects.equals(record.getModuleAlias(), pathVariable(request, "moduleAlias"));
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "UI set does not belong to module: " + pathVariable(request, "moduleAlias") + "." + id;
    }
}
