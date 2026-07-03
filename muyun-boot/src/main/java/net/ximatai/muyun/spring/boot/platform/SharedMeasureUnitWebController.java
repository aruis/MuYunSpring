package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.WebRequestScope;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebOutputSupport;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.platform.measure.MeasureUnit;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitCategoryService;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitConversion;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitConversionService;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.Objects;

@ApplicationScoped
@Path("/platform.measure_unit/categories/{categoryAlias}/units")
public class SharedMeasureUnitWebController extends NestedEnabledSortableCrudWebSupport<MeasureUnit, MeasureUnitService> {

    private final MeasureUnitConversionService conversionService;

    public SharedMeasureUnitWebController(MeasureUnitConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    protected void appendScope(Criteria criteria, WebRequestScope request) {
        criteria.eq("applicationAlias", MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS);
        criteria.eq("categoryAlias", categoryAlias(request));
    }

    @Override
    protected void bindScope(MeasureUnit record, WebRequestScope request) {
        record.setApplicationAlias(MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS);
        record.setCategoryAlias(categoryAlias(request));
    }

    @Override
    protected boolean inScope(MeasureUnit record, WebRequestScope request) {
        return Objects.equals(record.getApplicationAlias(), MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS)
                && Objects.equals(record.getCategoryAlias(), categoryAlias(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(WebRequestScope request, String id) {
        return "shared measure unit does not belong to category: " + categoryAlias(request) + "." + id;
    }

    @GET
    @Path("/options")
    @ActionEndpoint(PlatformAction.QUERY)
    public WebListResponse<MeasureUnit> options(@Context UriInfo uriInfo,
                                                @DefaultValue("true") @QueryParam("enabledOnly") boolean enabledOnly) {
        WebRequestScope request = requestScope(uriInfo);
        return webScope(() -> new WebListResponse<>(WebOutputSupport.records(service(),
                service().listUnits(MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS, categoryAlias(request), enabledOnly),
                FieldOutputContext.LIST)));
    }

    @POST
    @Path("/convert")
    @ActionEndpoint(PlatformAction.QUERY)
    public MeasureUnitConversion convert(@Context UriInfo uriInfo,
                                         MeasureUnitConversionRequest body) {
        WebRequestScope request = requestScope(uriInfo);
        return webScope(() -> conversionService.convert(
                MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS,
                categoryAlias(request),
                body.value(),
                body.fromUnitCode(),
                body.toUnitCode()));
    }

    private String categoryAlias(WebRequestScope request) {
        String value = pathVariable(request, "categoryAlias");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("categoryAlias is required");
        }
        return value;
    }

    public record MeasureUnitConversionRequest(BigDecimal value, String fromUnitCode, String toUnitCode) {
    }
}
