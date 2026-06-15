package net.ximatai.muyun.spring.boot.platform;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitCategory;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitCategoryService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.Set;

@RestController
@PlatformStaticModule(application = "platform", alias = MeasureUnitCategoryService.MODULE_ALIAS, title = "平台计量单位分类")
@RequestMapping("/platform.application/{applicationAlias}/measure-unit-categories")
public class MeasureUnitCategoryWebController
        extends NestedEnabledSortableCrudWebSupport<MeasureUnitCategory, MeasureUnitCategoryService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "id", "applicationAlias", "alias", "dimension", "baseUnitCode", "title",
            "enabled", "sortOrder", "createdAt", "updatedAt");

    @Override
    protected Criteria queryCriteria(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.criteria(request, QUERY_FIELDS, webScopeName());
    }

    @Override
    protected Sort[] querySorts(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.sorts(request, QUERY_FIELDS, Sort.asc("sortOrder"), Sort.asc("title"));
    }

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        criteria.eq("applicationAlias", applicationAlias(request));
    }

    @Override
    protected void bindScope(MeasureUnitCategory record, HttpServletRequest request) {
        record.setApplicationAlias(applicationAlias(request));
    }

    @Override
    protected boolean inScope(MeasureUnitCategory record, HttpServletRequest request) {
        return Objects.equals(record.getApplicationAlias(), applicationAlias(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "measure unit category does not belong to application: " + applicationAlias(request) + "." + id;
    }

    private String applicationAlias(HttpServletRequest request) {
        String value = pathVariable(request, "applicationAlias");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("applicationAlias is required");
        }
        return value;
    }
}
