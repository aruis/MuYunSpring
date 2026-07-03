package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.WebRequestScope;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebOutputSupport;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitCategory;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitCategoryService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;

@ApplicationScoped
@Path("/platform.measure_unit/categories")
public class SharedMeasureUnitCategoryWebController
        extends NestedEnabledSortableCrudWebSupport<MeasureUnitCategory, MeasureUnitCategoryService> {

    @Override
    protected void appendScope(Criteria criteria, WebRequestScope request) {
        criteria.eq("applicationAlias", MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS);
    }

    @Override
    protected void bindScope(MeasureUnitCategory record, WebRequestScope request) {
        record.setApplicationAlias(MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS);
    }

    @Override
    protected boolean inScope(MeasureUnitCategory record, WebRequestScope request) {
        return Objects.equals(record.getApplicationAlias(), MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS);
    }

    @Override
    protected String scopedRecordNotFoundMessage(WebRequestScope request, String id) {
        return "shared measure unit category does not exist: " + id;
    }

    @GET
    @Path("/options")
    @ActionEndpoint(PlatformAction.QUERY)
    public WebListResponse<MeasureUnitCategory> options(
            @DefaultValue("true") @QueryParam("enabledOnly") boolean enabledOnly) {
        return webScope(() -> new WebListResponse<>(WebOutputSupport.records(service(),
                service().listCategories(MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS, enabledOnly),
                FieldOutputContext.LIST)));
    }
}
