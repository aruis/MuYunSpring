package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.platform.currency.CurrencyConversion;
import net.ximatai.muyun.spring.platform.currency.CurrencyConversionService;
import net.ximatai.muyun.spring.platform.currency.ExchangeRate;
import net.ximatai.muyun.spring.platform.currency.ExchangeRateService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Supplier;

@RestController
@PlatformStaticModule(application = "platform", alias = ExchangeRateService.MODULE_ALIAS, title = "平台汇率")
@RequestMapping({"/platform.exchange_rate", "/platform.exchange-rates"})
public class ExchangeRateWebController extends NestedEnabledSortableCrudWebSupport<ExchangeRate, ExchangeRateService> {

    private final CurrencyConversionService conversionService;

    public ExchangeRateWebController(CurrencyConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public <T> T webScope(Supplier<T> action) {
        return action.get();
    }

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
    }

    @Override
    protected void bindScope(ExchangeRate record, HttpServletRequest request) {
    }

    @Override
    protected boolean inScope(ExchangeRate record, HttpServletRequest request) {
        return true;
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "exchange rate not found: " + id;
    }

    @PostMapping("/convert")
    @ActionEndpoint(PlatformAction.QUERY)
    public CurrencyConversion convert(@RequestBody CurrencyConversionRequest body) {
        return webScope(() -> conversionService.convert(
                body.amount(), body.fromCurrencyCode(), body.toCurrencyCode(), body.rateTypeCode(), body.rateDate()));
    }

    public record CurrencyConversionRequest(
            BigDecimal amount,
            String fromCurrencyCode,
            String toCurrencyCode,
            String rateTypeCode,
            LocalDate rateDate
    ) {
    }
}
