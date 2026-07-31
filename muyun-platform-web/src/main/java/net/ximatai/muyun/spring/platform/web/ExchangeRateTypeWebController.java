package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.platform.module.PlatformStaticModule;

import net.ximatai.muyun.database.core.orm.Criteria;
import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.web.WebListResponse;
import net.ximatai.muyun.spring.web.WebOutputSupport;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.platform.currency.ExchangeRateType;
import net.ximatai.muyun.spring.platform.currency.ExchangeRateTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

@RestController
@PlatformStaticWebScope(PlatformStaticWebScope.Scope.CUSTOM)
@PlatformStaticModule(application = net.ximatai.muyun.spring.platform.application.PlatformApplication.class, alias = ExchangeRateTypeService.MODULE_ALIAS,
        title = "平台汇率类型")
@RequestMapping({"/platform.exchange_rate_type", "/platform.exchange-rate-types"})
public class ExchangeRateTypeWebController
        extends NestedEnabledSortableCrudWebSupport<ExchangeRateType, ExchangeRateTypeService> {

    @Override
    public <T> T webScope(Supplier<T> action) {
        return action.get();
    }

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
    }

    @Override
    protected void bindScope(ExchangeRateType record, HttpServletRequest request) {
    }

    @Override
    protected boolean inScope(ExchangeRateType record, HttpServletRequest request) {
        return true;
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "exchange rate type not found: " + id;
    }

    @GetMapping("/options")
    @ActionEndpoint(PlatformAction.QUERY)
    public WebListResponse<ExchangeRateType> options(@RequestParam(defaultValue = "true") boolean enabledOnly) {
        return webScope(() -> new WebListResponse<>(WebOutputSupport.records(service(),
                service().listRateTypes(enabledOnly), FieldOutputContext.LIST)));
    }
}
