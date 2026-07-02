package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebOutputSupport;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.platform.currency.Currency;
import net.ximatai.muyun.spring.platform.currency.CurrencyService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;

import java.util.function.Supplier;

@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = CurrencyService.MODULE_ALIAS, title = "平台币种")
@Path("/platform.currency")
public class CurrencyWebController extends NestedEnabledSortableCrudWebSupport<Currency, CurrencyService> {

    @Override
    public <T> T webScope(Supplier<T> action) {
        return action.get();
    }

    @Override
    protected void appendScope(Criteria criteria, @Context HttpServletRequest request) {
    }

    @Override
    protected void bindScope(Currency record, @Context HttpServletRequest request) {
    }

    @Override
    protected boolean inScope(Currency record, @Context HttpServletRequest request) {
        return true;
    }

    @Override
    protected String scopedRecordNotFoundMessage(@Context HttpServletRequest request, String id) {
        return "currency not found: " + id;
    }

    @GET
    @Path("/options")
    @ActionEndpoint(PlatformAction.QUERY)
    public WebListResponse<Currency> options(@DefaultValue("true") @QueryParam("enabledOnly") boolean enabledOnly) {
        return webScope(() -> new WebListResponse<>(WebOutputSupport.records(service(),
                service().listVisibleCurrencies(enabledOnly), FieldOutputContext.LIST)));
    }
}
