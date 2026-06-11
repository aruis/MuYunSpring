package net.ximatai.muyun.spring.boot.platform;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.platform.ui.PlatformQueryItem;
import net.ximatai.muyun.spring.platform.ui.PlatformQueryItemService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.Set;

@RestController
@PlatformStaticModule(application = "platform", alias = PlatformQueryItemService.MODULE_ALIAS, title = "平台查询项")
@RequestMapping("/platform.query-template/{queryTemplateId}/items")
public class PlatformQueryItemWebController
        extends NestedEnabledSortableCrudWebSupport<PlatformQueryItem, PlatformQueryItemService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "title", "parentId", "moduleMetadataFieldId", "operator", "allowExternalValue",
            "externalValueKey", "enabled");

    @Override
    protected Criteria queryCriteria(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.criteria(request, QUERY_FIELDS, "platform query item query");
    }

    @Override
    protected Sort[] querySorts(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.sorts(request, QUERY_FIELDS, Sort.asc("sortOrder"), Sort.asc("title"));
    }

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        criteria.eq("queryTemplateId", pathVariable(request, "queryTemplateId"));
    }

    @Override
    protected void bindScope(PlatformQueryItem record, HttpServletRequest request) {
        record.setQueryTemplateId(pathVariable(request, "queryTemplateId"));
    }

    @Override
    protected boolean inScope(PlatformQueryItem record, HttpServletRequest request) {
        return Objects.equals(record.getQueryTemplateId(), pathVariable(request, "queryTemplateId"));
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "Query item does not belong to query template: "
                + pathVariable(request, "queryTemplateId") + "." + id;
    }
}
