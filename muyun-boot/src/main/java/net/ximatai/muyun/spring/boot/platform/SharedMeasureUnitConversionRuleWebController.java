package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.WebRequestScope;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitBusinessConversion;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitBusinessConversionService;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitCategoryService;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitConversionContext;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitConversionRule;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitConversionRuleService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@ApplicationScoped
@Path("/platform.measure_unit/conversion-rules")
public class SharedMeasureUnitConversionRuleWebController
        extends NestedEnabledSortableCrudWebSupport<MeasureUnitConversionRule, MeasureUnitConversionRuleService> {

    private final MeasureUnitBusinessConversionService conversionService;

    public SharedMeasureUnitConversionRuleWebController(MeasureUnitBusinessConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    protected void appendScope(Criteria criteria, WebRequestScope request) {
        criteria.eq("applicationAlias", MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS);
    }

    @Override
    protected void bindScope(MeasureUnitConversionRule record, WebRequestScope request) {
        record.setApplicationAlias(MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS);
    }

    @Override
    protected boolean inScope(MeasureUnitConversionRule record, WebRequestScope request) {
        return Objects.equals(record.getApplicationAlias(), MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS);
    }

    @Override
    protected String scopedRecordNotFoundMessage(WebRequestScope request, String id) {
        return "shared measure unit conversion rule does not exist: " + id;
    }

    @POST
    @Path("/convert")
    @ActionEndpoint(PlatformAction.QUERY)
    public MeasureUnitBusinessConversion convert(MeasureBusinessConversionRequest body) {
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
