package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebOutputSupport;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitCategory;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitCategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@PlatformStaticModule(application = "platform", alias = MeasureUnitCategoryService.MODULE_ALIAS, title = "平台计量单位分类")
@RequestMapping("/platform.application/{applicationAlias}/measure-unit-categories")
public class MeasureUnitCategoryWebController
        extends NestedEnabledSortableCrudWebSupport<MeasureUnitCategory, MeasureUnitCategoryService> {

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

    @GetMapping("/options")
    @ActionEndpoint(PlatformAction.QUERY)
    public WebListResponse<MeasureUnitCategory> options(HttpServletRequest request,
                                                        @RequestParam(defaultValue = "true") boolean enabledOnly) {
        return webScope(() -> new WebListResponse<>(WebOutputSupport.records(service(),
                service().listVisibleCategories(applicationAlias(request), enabledOnly),
                FieldOutputContext.LIST)));
    }

    private String applicationAlias(HttpServletRequest request) {
        String value = pathVariable(request, "applicationAlias");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("applicationAlias is required");
        }
        return value;
    }
}
