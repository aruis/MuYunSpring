package net.ximatai.muyun.spring.boot.platform;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.platform.ui.PlatformQueryTemplate;
import net.ximatai.muyun.spring.platform.ui.PlatformQueryTemplateService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.Set;

@RestController
@PlatformStaticModule(application = "platform", alias = PlatformQueryTemplateService.MODULE_ALIAS,
        title = "平台查询模板")
@RequestMapping("/platform.module/{moduleAlias}/query-templates")
public class PlatformQueryTemplateWebController
        extends NestedEnabledSortableCrudWebSupport<PlatformQueryTemplate, PlatformQueryTemplateService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "alias", "title", "defaultTemplate", "published", "enabled");

    @Override
    protected Criteria queryCriteria(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.criteria(request, QUERY_FIELDS, "platform query template query");
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
    protected void bindScope(PlatformQueryTemplate record, HttpServletRequest request) {
        record.setModuleAlias(pathVariable(request, "moduleAlias"));
    }

    @Override
    protected boolean inScope(PlatformQueryTemplate record, HttpServletRequest request) {
        return Objects.equals(record.getModuleAlias(), pathVariable(request, "moduleAlias"));
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "Query template does not belong to module: " + pathVariable(request, "moduleAlias") + "." + id;
    }
}
