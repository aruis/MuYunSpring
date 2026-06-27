package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitBusinessConversion;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitBusinessConversionService;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitCategoryService;
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

@RestController
@RequestMapping("/platform.measure_unit/conversion-rules")
public class SharedMeasureUnitConversionRuleWebController
        extends NestedEnabledSortableCrudWebSupport<MeasureUnitConversionRule, MeasureUnitConversionRuleService> {

    private final MeasureUnitBusinessConversionService conversionService;

    public SharedMeasureUnitConversionRuleWebController(MeasureUnitBusinessConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        criteria.eq("applicationAlias", MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS);
    }

    @Override
    protected void bindScope(MeasureUnitConversionRule record, HttpServletRequest request) {
        record.setApplicationAlias(MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS);
    }

    @Override
    protected boolean inScope(MeasureUnitConversionRule record, HttpServletRequest request) {
        return Objects.equals(record.getApplicationAlias(), MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS);
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "shared measure unit conversion rule does not exist: " + id;
    }

    @PostMapping("/convert")
    @ActionEndpoint(PlatformAction.QUERY)
    public MeasureUnitBusinessConversion convert(HttpServletRequest request,
                                                 @RequestBody MeasureBusinessConversionRequest body) {
        return webScope(() -> conversionService.convert(
                new MeasureUnitConversionContext(applicationAlias(body),
                        body.moduleAlias(), body.contextObjectType(), body.contextObjectId(), body.operatedAt()),
                body.value(),
                body.fromCategoryAlias(),
                body.fromUnitCode(),
                body.toCategoryAlias(),
                body.toUnitCode()));
    }

    private String applicationAlias(MeasureBusinessConversionRequest body) {
        return body.applicationAlias() == null || body.applicationAlias().isBlank()
                ? MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS
                : body.applicationAlias();
    }

    public record MeasureBusinessConversionRequest(
            String applicationAlias,
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
