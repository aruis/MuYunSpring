package net.ximatai.muyun.spring.boot.platform;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaOperator;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.web.WebPageResponse;
import net.ximatai.muyun.spring.boot.web.WebRecordResponse;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.currency.Currency;
import net.ximatai.muyun.spring.platform.currency.CurrencyConversion;
import net.ximatai.muyun.spring.platform.currency.CurrencyConversionService;
import net.ximatai.muyun.spring.platform.currency.CurrencyService;
import net.ximatai.muyun.spring.platform.currency.ExchangeRateService;
import net.ximatai.muyun.spring.platform.currency.TenantCurrencySetting;
import net.ximatai.muyun.spring.platform.currency.TenantCurrencySettingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CurrencyWebControllerTest {
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldDeclareCurrencyAndExchangeRoutes() throws Exception {
        assertThat(CurrencyWebController.class.getAnnotation(Path.class).value()).isEqualTo("/platform.currency");
        assertEndpoint(CurrencyWebController.class.getMethod("options", boolean.class),
                GET.class, "/options", PlatformAction.QUERY);

        assertThat(ExchangeRateWebController.class.getAnnotation(Path.class).value())
                .isEqualTo("/platform.exchange_rate");
        assertEndpoint(ExchangeRateWebController.class.getMethod("convert",
                        ExchangeRateWebController.CurrencyConversionRequest.class),
                POST.class, "/convert", PlatformAction.QUERY);

        assertThat(TenantCurrencySettingWebController.class.getAnnotation(Path.class).value())
                .isEqualTo("/platform.tenant_currency_setting");
    }

    @Test
    void shouldKeepTenantContextWhenListingCurrencyOptions() throws Exception {
        CurrencyService service = mock(CurrencyService.class);
        CurrencyWebController controller = new CurrencyWebController();
        setService(controller, service);
        when(service.listVisibleCurrencies(true)).thenAnswer(invocation -> {
            assertThat(TenantContext.currentTenantId()).contains("tenant-a");
            return List.of(currency("USD"));
        });

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            assertThat(controller.options(true).records())
                    .singleElement()
                    .extracting(Currency::getCode)
                    .isEqualTo("USD");
        }
    }

    @Test
    void shouldKeepTenantContextWhenConvertingCurrency() throws Exception {
        ExchangeRateService service = mock(ExchangeRateService.class);
        CurrencyConversionService conversionService = mock(CurrencyConversionService.class);
        ExchangeRateWebController controller = new ExchangeRateWebController(conversionService);
        setService(controller, service);
        when(conversionService.convert(new BigDecimal("12.34"), "USD", "CNY", "SPOT",
                LocalDate.parse("2026-02-16"))).thenAnswer(invocation -> {
                    assertThat(TenantContext.currentTenantId()).contains("tenant-a");
                    return new CurrencyConversion("USD", "CNY", "SPOT", LocalDate.parse("2026-02-16"),
                            new BigDecimal("12.34"), new BigDecimal("7.2"), new BigDecimal("88.85"));
                });

        CurrencyConversion conversion;
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            conversion = controller.convert(new ExchangeRateWebController.CurrencyConversionRequest(
                    new BigDecimal("12.34"), "USD", "CNY", "SPOT", LocalDate.parse("2026-02-16")));
        }

        assertThat(conversion.exchangeRate()).isEqualByComparingTo("7.2");
        assertThat(conversion.convertedAmount()).isEqualByComparingTo("88.85");
    }

    @Test
    void shouldQueryTenantCurrencySettingWithinCurrentTenant() throws Exception {
        TenantCurrencySettingService service = mock(TenantCurrencySettingService.class);
        TenantCurrencySettingWebController controller = new TenantCurrencySettingWebController();
        setService(controller, service);
        TenantCurrencySetting setting = setting("tenant-a", "CNY");
        when(service.pageQuery(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(setting), 1, PageRequest.of(1, 20)));

        WebPageResponse<TenantCurrencySetting> response;
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            response = controller.query(null, null);
        }

        assertThat(response.records()).containsExactly(setting);
        ArgumentCaptor<Criteria> criteria = ArgumentCaptor.forClass(Criteria.class);
        verify(service).pageQuery(criteria.capture(), any(PageRequest.class), any(Sort[].class));
        assertClause(criteria.getValue(), "tenantId", "tenant-a");
    }

    @Test
    void shouldKeepTenantContextWhenCreatingTenantCurrencySetting() throws Exception {
        TenantCurrencySettingService service = mock(TenantCurrencySettingService.class);
        TenantCurrencySettingWebController controller = new TenantCurrencySettingWebController();
        setService(controller, service);
        when(service.insert(any(TenantCurrencySetting.class))).thenAnswer(invocation -> {
            assertThat(TenantContext.currentTenantId()).contains("tenant-a");
            return "setting-1";
        });
        when(service.select("setting-1")).thenReturn(setting("tenant-a", "CNY"));

        WebRecordResponse<TenantCurrencySetting> response;
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            response = controller.insert(null, setting(null, "CNY"));
        }

        assertThat(response.record().getTenantId()).isEqualTo("tenant-a");
        assertThat(response.record().getBaseCurrencyCode()).isEqualTo("CNY");
    }

    private Currency currency(String code) {
        Currency currency = new Currency();
        currency.setCode(code);
        currency.setTitle(code);
        return currency;
    }

    private TenantCurrencySetting setting(String tenantId, String baseCurrencyCode) {
        TenantCurrencySetting setting = new TenantCurrencySetting();
        setting.setId("setting-1");
        setting.setTenantId(tenantId);
        setting.setBaseCurrencyCode(baseCurrencyCode);
        setting.setTitle("Base");
        return setting;
    }

    private void assertEndpoint(Method method,
                                Class<?> httpMethod,
                                String path,
                                PlatformAction action) {
        assertThat(method.getAnnotation(httpMethod.asSubclass(java.lang.annotation.Annotation.class))).isNotNull();
        assertThat(method.getAnnotation(Path.class).value()).isEqualTo(path);
        assertThat(method.getAnnotation(ActionEndpoint.class).value()).isEqualTo(action);
    }

    private void assertClause(Criteria criteria, String fieldName, Object expected) {
        boolean matched = criteria.getClauses().stream()
                .anyMatch(clause -> fieldName.equals(clause.getField())
                        && clause.getOperator() == CriteriaOperator.EQ
                        && clause.getValues().contains(expected));
        assertThat(matched).isTrue();
    }

    private void setService(Object controller, Object service) throws ReflectiveOperationException {
        Field field = WebSupport.class.getDeclaredField("service");
        field.setAccessible(true);
        field.set(controller, service);
    }
}
