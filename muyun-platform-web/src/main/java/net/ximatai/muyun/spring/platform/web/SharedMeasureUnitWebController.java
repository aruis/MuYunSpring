package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.database.core.orm.Criteria;
import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.web.WebListResponse;
import net.ximatai.muyun.spring.web.WebOutputSupport;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.platform.measure.MeasureUnit;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitCategoryService;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitConversion;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitConversionService;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Objects;

@RestController
@PlatformStaticWebProjection(module = MeasureUnitService.MODULE_ALIAS)
@RequestMapping("/platform.measure_unit/categories/{categoryAlias}/units")
public class SharedMeasureUnitWebController extends NestedEnabledSortableCrudWebSupport<MeasureUnit, MeasureUnitService> {

    private final MeasureUnitConversionService conversionService;

    public SharedMeasureUnitWebController(MeasureUnitConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        criteria.eq("applicationAlias", MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS);
        criteria.eq("categoryAlias", categoryAlias(request));
    }

    @Override
    protected void bindScope(MeasureUnit record, HttpServletRequest request) {
        record.setApplicationAlias(MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS);
        record.setCategoryAlias(categoryAlias(request));
    }

    @Override
    protected boolean inScope(MeasureUnit record, HttpServletRequest request) {
        return Objects.equals(record.getApplicationAlias(), MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS)
                && Objects.equals(record.getCategoryAlias(), categoryAlias(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "shared measure unit does not belong to category: " + categoryAlias(request) + "." + id;
    }

    @GetMapping("/options")
    @ActionEndpoint(PlatformAction.QUERY)
    public WebListResponse<MeasureUnit> options(HttpServletRequest request,
                                                @RequestParam(defaultValue = "true") boolean enabledOnly) {
        return webScope(() -> new WebListResponse<>(WebOutputSupport.records(service(),
                service().listUnits(MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS, categoryAlias(request), enabledOnly),
                FieldOutputContext.LIST)));
    }

    @PostMapping("/convert")
    @ActionEndpoint(PlatformAction.QUERY)
    public MeasureUnitConversion convert(HttpServletRequest request,
                                         @RequestBody MeasureUnitConversionRequest body) {
        return webScope(() -> conversionService.convert(
                MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS,
                categoryAlias(request),
                body.value(),
                body.fromUnitCode(),
                body.toUnitCode()));
    }

    private String categoryAlias(HttpServletRequest request) {
        String value = pathVariable(request, "categoryAlias");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("categoryAlias is required");
        }
        return value;
    }

    public record MeasureUnitConversionRequest(BigDecimal value, String fromUnitCode, String toUnitCode) {
    }
}
