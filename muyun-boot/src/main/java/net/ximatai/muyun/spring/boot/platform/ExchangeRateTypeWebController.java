package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.WebRequestScope;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebOutputSupport;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.platform.currency.ExchangeRateType;
import net.ximatai.muyun.spring.platform.currency.ExchangeRateTypeService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.function.Supplier;

@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = ExchangeRateTypeService.MODULE_ALIAS,
        title = "平台汇率类型")
@Path("/platform.exchange_rate_type")
public class ExchangeRateTypeWebController
        extends NestedEnabledSortableCrudWebSupport<ExchangeRateType, ExchangeRateTypeService> {

    @Override
    public <T> T webScope(Supplier<T> action) {
        return action.get();
    }

    @Override
    protected void appendScope(Criteria criteria, WebRequestScope request) {
    }

    @Override
    protected void bindScope(ExchangeRateType record, WebRequestScope request) {
    }

    @Override
    protected boolean inScope(ExchangeRateType record, WebRequestScope request) {
        return true;
    }

    @Override
    protected String scopedRecordNotFoundMessage(WebRequestScope request, String id) {
        return "exchange rate type not found: " + id;
    }

    @GET
    @Path("/options")
    @ActionEndpoint(PlatformAction.QUERY)
    public WebListResponse<ExchangeRateType> options(@DefaultValue("true") @QueryParam("enabledOnly") boolean enabledOnly) {
        return webScope(() -> new WebListResponse<>(WebOutputSupport.records(service(),
                service().listRateTypes(enabledOnly), FieldOutputContext.LIST)));
    }
}
