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
@RequestMapping("/platform.measure_unit/categories")
public class SharedMeasureUnitCategoryWebController
        extends NestedEnabledSortableCrudWebSupport<MeasureUnitCategory, MeasureUnitCategoryService> {

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        criteria.eq("applicationAlias", MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS);
    }

    @Override
    protected void bindScope(MeasureUnitCategory record, HttpServletRequest request) {
        record.setApplicationAlias(MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS);
    }

    @Override
    protected boolean inScope(MeasureUnitCategory record, HttpServletRequest request) {
        return Objects.equals(record.getApplicationAlias(), MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS);
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "shared measure unit category does not exist: " + id;
    }

    @GetMapping("/options")
    @ActionEndpoint(PlatformAction.QUERY)
    public WebListResponse<MeasureUnitCategory> options(HttpServletRequest request,
                                                        @RequestParam(defaultValue = "true") boolean enabledOnly) {
        return webScope(() -> new WebListResponse<>(WebOutputSupport.records(service(),
                service().listCategories(MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS, enabledOnly),
                FieldOutputContext.LIST)));
    }
}
