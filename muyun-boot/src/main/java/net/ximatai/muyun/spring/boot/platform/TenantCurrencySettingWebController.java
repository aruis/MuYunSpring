package net.ximatai.muyun.spring.boot.platform;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.NestedCrudWebSupport;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.currency.TenantCurrencySetting;
import net.ximatai.muyun.spring.platform.currency.TenantCurrencySettingService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.function.Supplier;

@RestController
@PlatformStaticModule(application = "platform", alias = TenantCurrencySettingService.MODULE_ALIAS,
        title = "平台租户币种设置")
@RequestMapping({"/platform.tenant_currency_setting", "/platform.tenant-currency-settings"})
public class TenantCurrencySettingWebController
        extends NestedCrudWebSupport<TenantCurrencySetting, TenantCurrencySettingService> {
    @Override
    public <T> T webScope(Supplier<T> action) {
        return action.get();
    }

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        criteria.eq("tenantId", TenantContext.currentTenantId()
                .orElseThrow(() -> new IllegalArgumentException("tenant currency setting requires tenant context")));
    }

    @Override
    protected void bindScope(TenantCurrencySetting record, HttpServletRequest request) {
    }

    @Override
    protected boolean inScope(TenantCurrencySetting record, HttpServletRequest request) {
        return Objects.equals(record.getTenantId(), TenantContext.currentTenantId().orElse(null));
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "tenant currency setting not found: " + id;
    }
}
