package net.ximatai.muyun.spring.boot.platform;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitBusinessConversion;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitBusinessConversionService;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitConversionContext;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitConversionRule;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitConversionRuleService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

@RestController
@PlatformStaticModule(application = "platform", alias = MeasureUnitConversionRuleService.MODULE_ALIAS,
        title = "平台计量单位换算规则")
@RequestMapping("/platform.application/{applicationAlias}/measure-unit-conversion-rules")
public class MeasureUnitConversionRuleWebController
        extends NestedEnabledSortableCrudWebSupport<MeasureUnitConversionRule, MeasureUnitConversionRuleService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "id", "applicationAlias", "scopeType", "moduleAlias", "contextObjectType", "contextObjectId",
            "fromCategoryAlias", "fromUnitCode", "toCategoryAlias", "toUnitCode",
            "factor", "priority", "effectiveFrom", "effectiveTo", "title", "enabled",
            "sortOrder", "createdAt", "updatedAt");

    private final MeasureUnitBusinessConversionService conversionService;

    public MeasureUnitConversionRuleWebController(MeasureUnitBusinessConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    protected Criteria queryCriteria(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.criteria(request, QUERY_FIELDS, webScopeName());
    }

    @Override
    protected Sort[] querySorts(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.sorts(request, QUERY_FIELDS,
                Sort.desc("priority"), Sort.asc("sortOrder"), Sort.asc("title"));
    }

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        criteria.eq("applicationAlias", applicationAlias(request));
    }

    @Override
    protected void bindScope(MeasureUnitConversionRule record, HttpServletRequest request) {
        record.setApplicationAlias(applicationAlias(request));
    }

    @Override
    protected boolean inScope(MeasureUnitConversionRule record, HttpServletRequest request) {
        return Objects.equals(record.getApplicationAlias(), applicationAlias(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "measure unit conversion rule does not belong to application: "
                + applicationAlias(request) + "." + id;
    }

    @PostMapping("/convert")
    @ActionEndpoint(PlatformAction.QUERY)
    public MeasureUnitBusinessConversion convert(HttpServletRequest request,
                                                 @RequestBody MeasureBusinessConversionRequest body) {
        return webScope(() -> conversionService.convert(
                new MeasureUnitConversionContext(applicationAlias(request), body.moduleAlias(),
                        body.contextObjectType(), body.contextObjectId(), body.operatedAt()),
                body.value(),
                body.fromCategoryAlias(),
                body.fromUnitCode(),
                body.toCategoryAlias(),
                body.toUnitCode()));
    }

    private String applicationAlias(HttpServletRequest request) {
        String value = pathVariable(request, "applicationAlias");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("applicationAlias is required");
        }
        return value;
    }

    public record MeasureBusinessConversionRequest(
            String moduleAlias,
            String contextObjectType,
            String contextObjectId,
            LocalDateTime operatedAt,
            BigDecimal value,
            String fromCategoryAlias,
            String fromUnitCode,
            String toCategoryAlias,
            String toUnitCode
    ) {
    }
}
