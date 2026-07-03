package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.WebRequestScope;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitBusinessConversion;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitBusinessConversionService;
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
@PlatformStaticModule(application = "platform", alias = MeasureUnitConversionRuleService.MODULE_ALIAS,
        title = "平台计量单位换算规则")
@Path("/platform.application/{applicationAlias}/measure-unit-conversion-rules")
public class MeasureUnitConversionRuleWebController
        extends NestedEnabledSortableCrudWebSupport<MeasureUnitConversionRule, MeasureUnitConversionRuleService> {

    private final MeasureUnitBusinessConversionService conversionService;

    public MeasureUnitConversionRuleWebController(MeasureUnitBusinessConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    protected void appendScope(Criteria criteria, WebRequestScope request) {
        criteria.eq("applicationAlias", applicationAlias(request));
    }

    @Override
    protected void bindScope(MeasureUnitConversionRule record, WebRequestScope request) {
        record.setApplicationAlias(applicationAlias(request));
    }

    @Override
    protected boolean inScope(MeasureUnitConversionRule record, WebRequestScope request) {
        return Objects.equals(record.getApplicationAlias(), applicationAlias(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(WebRequestScope request, String id) {
        return "measure unit conversion rule does not belong to application: "
                + applicationAlias(request) + "." + id;
    }

    @POST
    @Path("/convert")
    @ActionEndpoint(PlatformAction.QUERY)
    public MeasureUnitBusinessConversion convert(@Context UriInfo uriInfo,
                                                 MeasureBusinessConversionRequest body) {
        WebRequestScope request = requestScope(uriInfo);
        return webScope(() -> conversionService.convert(
                new MeasureUnitConversionContext(applicationAlias(request), body.moduleAlias(),
                        body.contextObjectType(), body.contextObjectId(), body.operatedAt()),
                body.value(),
                body.fromCategoryAlias(),
                body.fromUnitCode(),
                body.toCategoryAlias(),
                body.toUnitCode()));
    }

    private String applicationAlias(WebRequestScope request) {
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
