package net.ximatai.muyun.spring.platform.currency;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.spring.common.platform.AllowAllActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.AllowAllDataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldMoneyDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldMoneyMode;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicFieldValueValidator;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicModuleRegistry;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordRuntime;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MoneyDynamicRecordServiceIntegrationTest {
    private static final String SCHEMA = "public";
    private static final String MODULE = "sales.order";

    private final CurrencyService currencyService = new CurrencyService(new TestMemoryDao<>());
    private final ExchangeRateTypeService rateTypeService = new ExchangeRateTypeService(new TestMemoryDao<>());
    private final ExchangeRateService rateService =
            new ExchangeRateService(new TestMemoryDao<>(), currencyService, rateTypeService);
    private final TenantCurrencySettingService tenantCurrencySettingService =
            new TenantCurrencySettingService(new TestMemoryDao<>(), currencyService);
    private final MoneyDynamicRecordMutationCoordinator coordinator =
            new MoneyDynamicRecordMutationCoordinator(
                    new CurrencyConversionService(currencyService, rateService),
                    tenantCurrencySettingService,
                    Clock.fixed(Instant.parse("2026-06-16T00:00:00Z"), ZoneOffset.UTC)
            );

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldPersistNormalizedMoneyFieldsThroughDynamicCreateService() {
        prepareCurrenciesAndRates();
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), eq("sales_order"), anyMap(), eq(StandardEntitySchema.ID_COLUMN)))
                .thenReturn("order-1");
        DynamicRecordService service = service(operations);
        DynamicRecord record = service.newRecord(MODULE, "order")
                .setValue("amount", new BigDecimal("2"))
                .setValue("currencyCode", " usd ")
                .setValue("orderDate", LocalDate.of(2026, 2, 16));

        String id = service.create(MODULE, "order", record);

        assertThat(id).isEqualTo("order-1");
        ArgumentCaptor<Map<String, Object>> body = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(operations)
                .insertItem(eq(SCHEMA), eq("sales_order"), body.capture(), eq(StandardEntitySchema.ID_COLUMN));
        assertThat(body.getValue())
                .containsEntry("currency_code", "USD")
                .containsEntry("amount", new BigDecimal("2"))
                .containsEntry("exchange_rate", new BigDecimal("7.2345"));
        assertThat((BigDecimal) body.getValue().get("base_amount")).isEqualByComparingTo("14.47");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldPersistNormalizedMoneyFieldsThroughDynamicUpdateService() {
        prepareCurrenciesAndRates();
        IDatabaseOperations<Object> operations = operations();
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(beforeRow()));
        when(operations.patchUpdateItemWhere(eq(SCHEMA), eq("sales_order"), anyMap(), anyMap(),
                eq(StandardEntitySchema.ID_COLUMN))).thenReturn(1);
        DynamicRecordService service = service(operations);
        DynamicRecord record = service.newRecord(MODULE, "order")
                .setValue("amount", new BigDecimal("2"));
        record.setId("order-1");

        int updated = service.update(MODULE, "order", record);

        assertThat(updated).isEqualTo(1);
        ArgumentCaptor<Map<String, Object>> body = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, Object>> where = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(operations)
                .patchUpdateItemWhere(eq(SCHEMA), eq("sales_order"), body.capture(), where.capture(),
                        eq(StandardEntitySchema.ID_COLUMN));
        assertThat(body.getValue())
                .containsEntry("amount", new BigDecimal("2"))
                .containsEntry("exchange_rate", new BigDecimal("7.2345"));
        assertThat((BigDecimal) body.getValue().get("base_amount")).isEqualByComparingTo("14.47");
        assertThat(where.getValue())
                .containsEntry(StandardEntitySchema.ID_COLUMN, "order-1")
                .containsEntry(StandardEntitySchema.VERSION_COLUMN, 1);
    }

    private DynamicRecordService service(IDatabaseOperations<Object> operations) {
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(
                operations,
                new DynamicModuleRegistry(),
                DynamicFieldValueValidator.NONE,
                null
        ).register(new ModuleDefinition(MODULE, "Order", List.of(orderEntity())));
        return new DynamicRecordService(
                runtime,
                new AllowAllActionExecutionPolicyService(),
                new AllowAllDataScopeCriteriaService(),
                coordinator
        );
    }

    @SuppressWarnings("unchecked")
    private IDatabaseOperations<Object> operations() {
        IDatabaseOperations<Object> operations = mock(IDatabaseOperations.class);
        when(operations.getDBInfo()).thenReturn(new DBInfo("POSTGRESQL").setName("muyun_test"));
        when(operations.getDefaultSchemaName()).thenReturn(SCHEMA);
        return operations;
    }

    private EntityDefinition orderEntity() {
        return new EntityDefinition(
                "order",
                "sales_order",
                "Order",
                List.of(
                        FieldDefinition.decimal("amount", "Amount").money(new FieldMoneyDefinition(
                                FieldMoneyMode.SELECTABLE,
                                null,
                                null,
                                "currencyCode",
                                "baseAmount",
                                "CNY",
                                "SPOT",
                                "orderDate",
                                "exchangeRate",
                                true
                        )),
                        FieldDefinition.string("currencyCode", "Currency").column("currency_code").length(3),
                        FieldDefinition.decimal("baseAmount", "Base Amount").column("base_amount"),
                        FieldDefinition.of("orderDate", FieldType.DATE, "Order Date").column("order_date"),
                        FieldDefinition.decimal("exchangeRate", "Exchange Rate").column("exchange_rate")
                ),
                Set.of(EntityCapability.CRUD)
        );
    }

    private Map<String, Object> beforeRow() {
        return Map.ofEntries(
                Map.entry(StandardEntitySchema.ID_COLUMN, "order-1"),
                Map.entry(StandardEntitySchema.VERSION_COLUMN, 1),
                Map.entry(StandardEntitySchema.DELETED_COLUMN, Boolean.FALSE),
                Map.entry("amount", new BigDecimal("1")),
                Map.entry("currency_code", "USD"),
                Map.entry("base_amount", new BigDecimal("7.23")),
                Map.entry("order_date", LocalDate.of(2026, 2, 16)),
                Map.entry("exchange_rate", new BigDecimal("7.2345"))
        );
    }

    private void prepareCurrenciesAndRates() {
        currencyService.insert(currency("USD", "840", "US Dollar", "$", 2));
        currencyService.insert(currency("CNY", "156", "人民币", "¥", 2));
        rateTypeService.insert(rateType("SPOT", "Spot"));
        rateService.insert(rate("USD", "CNY", "SPOT", "2026-02-01", "7.2345"));
    }

    private Currency currency(String code, String numericCode, String title, String symbol, int scale) {
        Currency currency = new Currency();
        currency.setCode(code);
        currency.setNumericCode(numericCode);
        currency.setTitle(title);
        currency.setSymbol(symbol);
        currency.setDecimalScale(scale);
        return currency;
    }

    private ExchangeRateType rateType(String code, String title) {
        ExchangeRateType rateType = new ExchangeRateType();
        rateType.setCode(code);
        rateType.setTitle(title);
        return rateType;
    }

    private ExchangeRate rate(String from, String to, String type, String effectiveDate, String rateValue) {
        ExchangeRate rate = new ExchangeRate();
        rate.setFromCurrencyCode(from);
        rate.setToCurrencyCode(to);
        rate.setRateTypeCode(type);
        rate.setEffectiveDate(LocalDate.parse(effectiveDate));
        rate.setRate(new BigDecimal(rateValue));
        rate.setTitle(from + "/" + to + " " + type);
        return rate;
    }
}
