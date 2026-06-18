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
import net.ximatai.muyun.spring.dynamic.metadata.EntityRelationDefinition;
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
    void shouldPersistDefaultSelectableCurrencyThroughDynamicCreateService() {
        prepareCurrenciesAndRates();
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), eq("sales_order"), anyMap(), eq(StandardEntitySchema.ID_COLUMN)))
                .thenReturn("order-1");
        DynamicRecordService service = service(operations, orderEntityWithDefaultCurrency());
        DynamicRecord record = service.newRecord(MODULE, "order")
                .setValue("amount", new BigDecimal("2"))
                .setValue("orderDate", LocalDate.of(2026, 2, 16));

        service.create(MODULE, "order", record);

        ArgumentCaptor<Map<String, Object>> body = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(operations)
                .insertItem(eq(SCHEMA), eq("sales_order"), body.capture(), eq(StandardEntitySchema.ID_COLUMN));
        assertThat(body.getValue())
                .containsEntry("currency_code", "USD")
                .containsEntry("exchange_rate", new BigDecimal("7.2345"));
        assertThat((BigDecimal) body.getValue().get("base_amount")).isEqualByComparingTo("14.47");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldPersistTenantScopedBaseCurrencyThroughDynamicCreateService() {
        prepareCurrenciesAndRates();
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            tenantCurrencySettingService.insert(setting("CNY"));
            rateService.insert(rate("USD", "CNY", "SPOT", "2026-02-01", "8.0000"));
        }
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), eq("sales_order"), anyMap(), eq(StandardEntitySchema.ID_COLUMN)))
                .thenReturn("order-1");
        DynamicRecordService service = service(operations, tenantBaseCurrencyEntity());
        DynamicRecord record = service.newRecord(MODULE, "order")
                .setValue("amount", new BigDecimal("2"))
                .setValue("currencyCode", "USD")
                .setValue("orderDate", LocalDate.of(2026, 2, 16));
        record.setTenantId("tenant-a");

        try (TenantContext.Scope ignored = TenantContext.system("system create")) {
            service.create(MODULE, "order", record);
        }

        ArgumentCaptor<Map<String, Object>> body = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(operations)
                .insertItem(eq(SCHEMA), eq("sales_order"), body.capture(), eq(StandardEntitySchema.ID_COLUMN));
        assertThat(body.getValue())
                .containsEntry("tenant_id", "tenant-a")
                .containsEntry("currency_code", "USD")
                .containsEntry("exchange_rate", new BigDecimal("8.0000"));
        assertThat((BigDecimal) body.getValue().get("base_amount")).isEqualByComparingTo("16.00");
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

    @Test
    @SuppressWarnings("unchecked")
    void shouldUsePersistedParentTenantWhenSystemUpdateCreatesMoneyRelationChild() {
        prepareCurrenciesAndRates();
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            tenantCurrencySettingService.insert(setting("CNY"));
        }
        IDatabaseOperations<Object> operations = operations();
        when(operations.query(anyString(), anyMap())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("\"sales_order_line\"") && sql.contains("\"order_id\" =")) {
                return List.of();
            }
            if (sql.contains("\"sales_order\"") && sql.contains("\"id\" =")) {
                return List.of(beforeRowWithTenant());
            }
            return List.of();
        });
        when(operations.patchUpdateItemWhere(eq(SCHEMA), eq("sales_order"), anyMap(), anyMap(),
                eq(StandardEntitySchema.ID_COLUMN))).thenReturn(1);
        when(operations.insertItem(eq(SCHEMA), eq("sales_order_line"), anyMap(), eq(StandardEntitySchema.ID_COLUMN)))
                .thenReturn("line-1");
        DynamicRecordService service = service(operations, orderWithLineModule());
        DynamicRecord child = service.newRecord(MODULE, "order_line")
                .setValue("amount", new BigDecimal("2"))
                .setValue("currencyCode", "USD")
                .setValue("orderDate", LocalDate.of(2026, 2, 16));
        DynamicRecord record = service.newRecord(MODULE, "order");
        record.setId("order-1");
        record.setVersion(1);
        record.setChildren("lines", List.of(child));

        try (TenantContext.Scope ignored = TenantContext.system("system writeback")) {
            assertThat(service.update(MODULE, "order", record)).isEqualTo(1);
        }

        ArgumentCaptor<Map<String, Object>> childBody = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(operations)
                .insertItem(eq(SCHEMA), eq("sales_order_line"), childBody.capture(), eq(StandardEntitySchema.ID_COLUMN));
        assertThat(childBody.getValue())
                .containsEntry("tenant_id", "tenant-a")
                .containsEntry("currency_code", "USD")
                .containsEntry("order_id", "order-1")
                .containsEntry("exchange_rate", new BigDecimal("7.2345"));
        assertThat((BigDecimal) childBody.getValue().get("base_amount")).isEqualByComparingTo("14.47");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldPersistRecalculatedMoneyFieldsWhenOnlyRateDateChangesThroughDynamicUpdateService() {
        prepareCurrenciesAndRates();
        rateService.insert(rate("USD", "CNY", "SPOT", "2026-03-01", "7.3000"));
        IDatabaseOperations<Object> operations = operations();
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(beforeRow()));
        when(operations.patchUpdateItemWhere(eq(SCHEMA), eq("sales_order"), anyMap(), anyMap(),
                eq(StandardEntitySchema.ID_COLUMN))).thenReturn(1);
        DynamicRecordService service = service(operations);
        DynamicRecord record = service.newRecord(MODULE, "order")
                .setValue("orderDate", LocalDate.of(2026, 3, 16));
        record.setId("order-1");

        service.update(MODULE, "order", record);

        ArgumentCaptor<Map<String, Object>> body = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(operations)
                .patchUpdateItemWhere(eq(SCHEMA), eq("sales_order"), body.capture(), anyMap(),
                        eq(StandardEntitySchema.ID_COLUMN));
        assertThat(body.getValue())
                .containsEntry("order_date", LocalDate.of(2026, 3, 16))
                .containsEntry("exchange_rate", new BigDecimal("7.3000"));
        assertThat((BigDecimal) body.getValue().get("base_amount")).isEqualByComparingTo("7.30");
    }

    private DynamicRecordService service(IDatabaseOperations<Object> operations) {
        return service(operations, new ModuleDefinition(MODULE, "Order", List.of(orderEntity())));
    }

    private DynamicRecordService service(IDatabaseOperations<Object> operations, EntityDefinition entity) {
        return service(operations, new ModuleDefinition(MODULE, "Order", List.of(entity)));
    }

    private DynamicRecordService service(IDatabaseOperations<Object> operations, ModuleDefinition module) {
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(
                operations,
                new DynamicModuleRegistry(),
                DynamicFieldValueValidator.NONE,
                null
        ).register(module);
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
        return orderEntity(null, "CNY");
    }

    private EntityDefinition orderEntityWithDefaultCurrency() {
        return orderEntity("USD", "CNY");
    }

    private EntityDefinition tenantBaseCurrencyEntity() {
        return orderEntity(null, null);
    }

    private EntityDefinition orderEntity(String defaultCurrencyCode, String baseCurrencyCode) {
        return new EntityDefinition(
                "order",
                "sales_order",
                "Order",
                List.of(
                        FieldDefinition.decimal("amount", "Amount").money(new FieldMoneyDefinition(
                                FieldMoneyMode.SELECTABLE,
                                null,
                                defaultCurrencyCode,
                                "currencyCode",
                                "baseAmount",
                                baseCurrencyCode,
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

    private ModuleDefinition orderWithLineModule() {
        return new ModuleDefinition(
                MODULE,
                "Order",
                List.of(orderEntity(), orderLineEntity()),
                List.of(EntityRelationDefinition.child("lines", "order", "order_line", "orderId")
                        .withAutoPopulate()
                        .withAutoDeleteWithParent())
        );
    }

    private EntityDefinition orderLineEntity() {
        return new EntityDefinition(
                "order_line",
                "sales_order_line",
                "Order Line",
                List.of(
                        FieldDefinition.string("orderId", "Order").column("order_id").length(64).required(),
                        FieldDefinition.decimal("amount", "Amount").money(new FieldMoneyDefinition(
                                FieldMoneyMode.SELECTABLE,
                                null,
                                null,
                                "currencyCode",
                                "baseAmount",
                                null,
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

    private Map<String, Object> beforeRowWithTenant() {
        return Map.ofEntries(
                Map.entry(StandardEntitySchema.ID_COLUMN, "order-1"),
                Map.entry(StandardEntitySchema.VERSION_COLUMN, 1),
                Map.entry(StandardEntitySchema.TENANT_ID_COLUMN, "tenant-a"),
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

    private TenantCurrencySetting setting(String baseCurrencyCode) {
        TenantCurrencySetting setting = new TenantCurrencySetting();
        setting.setBaseCurrencyCode(baseCurrencyCode);
        setting.setTitle("Tenant base currency");
        return setting;
    }
}
