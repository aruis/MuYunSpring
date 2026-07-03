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
@PlatformStaticModule(application = "platform", alias = MeasureUnitCategoryService.MODULE_ALIAS, title = "平台计量单位分类")
@Path("/platform.application/{applicationAlias}/measure-unit-categories")
public class MeasureUnitCategoryWebController
        extends NestedEnabledSortableCrudWebSupport<MeasureUnitCategory, MeasureUnitCategoryService> {

    @Override
    protected void appendScope(Criteria criteria, WebRequestScope request) {
        criteria.eq("applicationAlias", applicationAlias(request));
    }

    @Override
    protected void bindScope(MeasureUnitCategory record, WebRequestScope request) {
        record.setApplicationAlias(applicationAlias(request));
    }

    @Override
    protected boolean inScope(MeasureUnitCategory record, WebRequestScope request) {
        return Objects.equals(record.getApplicationAlias(), applicationAlias(request));
    }

    @Override
    protected String scopedRecordNotFoundMessage(WebRequestScope request, String id) {
        return "measure unit category does not belong to application: " + applicationAlias(request) + "." + id;
    }

    @GET
    @Path("/options")
    @ActionEndpoint(PlatformAction.QUERY)
    public WebListResponse<MeasureUnitCategory> options(@Context UriInfo uriInfo,
                                                        @DefaultValue("true") @QueryParam("enabledOnly") boolean enabledOnly) {
        WebRequestScope request = requestScope(uriInfo);
        return webScope(() -> new WebListResponse<>(WebOutputSupport.records(service(),
                service().listVisibleCategories(applicationAlias(request), enabledOnly),
                FieldOutputContext.LIST)));
    }

    private String applicationAlias(WebRequestScope request) {
        String value = pathVariable(request, "applicationAlias");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("applicationAlias is required");
        }
        return value;
    }
}
