package net.ximatai.muyun.spring.boot.platform;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.web.NestedCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.currency.TenantCurrencySetting;
import net.ximatai.muyun.spring.platform.currency.TenantCurrencySettingService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

@RestController
@PlatformStaticModule(application = "platform", alias = TenantCurrencySettingService.MODULE_ALIAS,
        title = "平台租户币种设置")
@RequestMapping({"/platform.tenant_currency_setting", "/platform.tenant-currency-settings"})
public class TenantCurrencySettingWebController
        extends NestedCrudWebSupport<TenantCurrencySetting, TenantCurrencySettingService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "id", "tenantId", "baseCurrencyCode", "title", "createdAt", "updatedAt");

    @Override
    public <T> T webScope(Supplier<T> action) {
        return action.get();
    }

    @Override
    protected Criteria queryCriteria(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.criteria(request, QUERY_FIELDS, webScopeName());
    }

    @Override
    protected Sort[] querySorts(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.sorts(request, QUERY_FIELDS, Sort.asc("tenantId"));
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
