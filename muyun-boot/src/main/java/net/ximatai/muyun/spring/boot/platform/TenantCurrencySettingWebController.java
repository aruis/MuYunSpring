package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.WebRequestScope;
import net.ximatai.muyun.spring.boot.web.NestedCrudWebSupport;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.currency.TenantCurrencySetting;
import net.ximatai.muyun.spring.platform.currency.TenantCurrencySettingService;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;
import java.util.function.Supplier;

@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = TenantCurrencySettingService.MODULE_ALIAS,
        title = "平台租户币种设置")
@Path("/platform.tenant_currency_setting")
public class TenantCurrencySettingWebController
        extends NestedCrudWebSupport<TenantCurrencySetting, TenantCurrencySettingService> {

    @Override
    public <T> T webScope(Supplier<T> action) {
        return action.get();
    }

    @Override
    protected void appendScope(Criteria criteria, WebRequestScope request) {
        criteria.eq("tenantId", TenantContext.currentTenantId()
                .orElseThrow(() -> new IllegalArgumentException("tenant currency setting requires tenant context")));
    }

    @Override
    protected void bindScope(TenantCurrencySetting record, WebRequestScope request) {
    }

    @Override
    protected boolean inScope(TenantCurrencySetting record, WebRequestScope request) {
        return Objects.equals(record.getTenantId(), TenantContext.currentTenantId().orElse(null));
    }

    @Override
    protected String scopedRecordNotFoundMessage(WebRequestScope request, String id) {
        return "tenant currency setting not found: " + id;
    }
}
