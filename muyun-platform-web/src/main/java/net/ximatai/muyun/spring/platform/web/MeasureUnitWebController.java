package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.platform.module.PlatformStaticModule;

import net.ximatai.muyun.database.core.orm.Criteria;
import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.web.WebListResponse;
import net.ximatai.muyun.spring.web.WebOutputSupport;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.platform.measure.MeasureUnit;
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
@PlatformStaticWebScope(PlatformStaticWebScope.Scope.CUSTOM)
@PlatformStaticModule(application = net.ximatai.muyun.spring.platform.application.PlatformApplication.class, alias = MeasureUnitService.MODULE_ALIAS, title = "平台计量单位")
@RequestMapping("/platform.application/{applicationAlias}/measure-unit-categories/{categoryAlias}/units")
public class MeasureUnitWebController extends NestedEnabledSortableCrudWebSupport<MeasureUnit, MeasureUnitService> {

    private final MeasureUnitConversionService conversionService;

    public MeasureUnitWebController(MeasureUnitConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        criteria.eq("applicationAlias", applicationAlias(request));
        criteria.eq("categoryAlias", categoryAlias(request));
    }

    @Override
    protected void bindScope(MeasureUnit record, HttpServletRequest request) {
        record.setApplicationAlias(applicationAlias(request));
        record.setCategoryAlias(categoryAlias(request));
    }

    @Override
    protected boolean inScope(MeasureUnit record, HttpServletRequest request) {
        return Objects.equals(record.getApplicationAlias(), applicationAlias(request))
                && Objects.equals(record.getCategoryAlias(), categoryAlias(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "measure unit does not belong to category: "
                + applicationAlias(request) + "." + categoryAlias(request) + "." + id;
    }

    @GetMapping("/options")
    @ActionEndpoint(PlatformAction.QUERY)
    public WebListResponse<MeasureUnit> options(HttpServletRequest request,
                                                @RequestParam(defaultValue = "true") boolean enabledOnly) {
        return webScope(() -> new WebListResponse<>(WebOutputSupport.records(service(),
                service().listVisibleUnits(applicationAlias(request), categoryAlias(request), enabledOnly),
                FieldOutputContext.LIST)));
    }

    @PostMapping("/convert")
    @ActionEndpoint(PlatformAction.QUERY)
    public MeasureUnitConversion convert(HttpServletRequest request,
                                         @RequestBody MeasureUnitConversionRequest body) {
        return webScope(() -> conversionService.convert(
                applicationAlias(request),
                categoryAlias(request),
                body.value(),
                body.fromUnitCode(),
                body.toUnitCode()));
    }

    private String applicationAlias(HttpServletRequest request) {
        String value = pathVariable(request, "applicationAlias");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("applicationAlias is required");
        }
        return value;
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
