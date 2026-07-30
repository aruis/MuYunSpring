package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import net.ximatai.muyun.spring.common.model.standard.StandardSortableEntity;
import net.ximatai.muyun.spring.common.money.MoneyField;
import net.ximatai.muyun.spring.ability.SortPartitionBy;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StaticMoneyFieldDefinitionCompilerTest {
    @Test
    void shouldCompileSelectableStaticMoneyField() {
        FieldDefinition compiled = StaticMoneyFieldDefinitionCompiler.compile(
                FieldDefinition.decimal("amount", "Amount").precision(18, 2),
                StaticOrder.class);

        assertThat(compiled.money()).satisfies(money -> {
            assertThat(money.enabled()).isTrue();
            assertThat(money.currencyMode()).isEqualTo(FieldMoneyMode.SELECTABLE);
            assertThat(money.defaultCurrencyCode()).isEqualTo("USD");
            assertThat(money.currencyFieldName()).isEqualTo("amountCurrency");
            assertThat(money.baseAmountFieldName()).isEqualTo("amountBase");
            assertThat(money.baseCurrencyCode()).isEqualTo("CNY");
            assertThat(money.rateTypeCode()).isEqualTo("SPOT");
            assertThat(money.rateDateFieldName()).isEqualTo("orderDate");
            assertThat(money.exchangeRateFieldName()).isEqualTo("amountRate");
            assertThat(money.currencyRequired()).isTrue();
        });
    }

    @Test
    void shouldCompileFixedStaticMoneyField() {
        FieldDefinition compiled = StaticMoneyFieldDefinitionCompiler.compile(
                FieldDefinition.decimal("fee", "Fee").precision(18, 2),
                StaticOrder.class);

        assertThat(compiled.money()).satisfies(money -> {
            assertThat(money.enabled()).isTrue();
            assertThat(money.currencyMode()).isEqualTo(FieldMoneyMode.FIXED);
            assertThat(money.fixedCurrencyCode()).isEqualTo("EUR");
            assertThat(money.currencyFieldName()).isNull();
            assertThat(money.baseAmountFieldName()).isEqualTo("feeBase");
            assertThat(money.rateTypeCode()).isEqualTo("BOOKING");
            assertThat(money.currencyRequired()).isFalse();
        });
    }

    @Test
    void shouldReturnDefinitionWhenStaticFieldHasNoMoneyAnnotation() {
        FieldDefinition definition = FieldDefinition.string("customerId", "Customer").length(64);

        FieldDefinition compiled = StaticMoneyFieldDefinitionCompiler.compile(definition, StaticOrder.class);

        assertThat(compiled).isSameAs(definition);
        assertThat(compiled.money().enabled()).isFalse();
    }

    @Test
    void shouldRejectSelectableStaticMoneyWithoutCurrencyFieldName() throws NoSuchFieldException {
        java.lang.reflect.Field field = InvalidSelectableMoney.class.getDeclaredField("amount");

        assertThatThrownBy(() -> StaticMoneyFieldDefinitionCompiler.money(field))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("selectable currency mode requires currencyFieldName");
    }

    @Test
    void shouldCompileStaticEntityDefinitionAndPassUnifiedMoneyValidation() {
        EntityDefinition entity = new StaticEntityDefinitionCompiler()
                .compile("order", "Order", StaticOrderEntity.class);

        new ModuleDefinitionValidator().validateEntity(entity);
        assertThat(entity.fields()).filteredOn(field -> field.fieldName().equals("amount"))
                .singleElement()
                .satisfies(field -> assertThat(field.money().baseAmountFieldName()).isEqualTo("amountBase"));
    }

    @Test
    void shouldCompileSortPartitionAsSortableStaticDefinition() {
        EntityDefinition entity = new StaticEntityDefinitionCompiler()
                .compile("sorted_order", "Sorted order", StaticSortedOrderEntity.class);

        new ModuleDefinitionValidator().validateEntity(entity);
        assertThat(entity.capabilities()).contains(EntityCapability.SORT);
        assertThat(entity.sortPartitionFields()).containsExactly("customerId");
        assertThat(entity.fields()).anySatisfy(field -> assertThat(field.isSortable()).isTrue());
    }

    @Test
    void shouldRejectInvalidDynamicMoneyDefinition() {
        EntityDefinition entity = new EntityDefinition("order", "sales_order", "Order",
                java.util.List.of(
                        FieldDefinition.decimal("amount", "Amount").precision(18, 2)
                                .money(new FieldMoneyDefinition(
                                        FieldMoneyMode.SELECTABLE,
                                        null,
                                        null,
                                        "amountCurrency",
                                        "amount",
                                        null,
                                        "SPOT",
                                        null,
                                        null,
                                        true
                                )),
                        FieldDefinition.string("amountCurrency", "Currency").column("amount_currency").length(3)
                ));

        assertThatThrownBy(() -> new ModuleDefinitionValidator().validateEntity(entity))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("base amount field must be different");
    }

    @Test
    void shouldNormalizeDynamicMoneyCodes() {
        FieldMoneyDefinition money = new FieldMoneyDefinition(
                FieldMoneyMode.FIXED,
                "usd",
                "eur",
                null,
                "amountBase",
                "cny",
                "spot",
                null,
                null,
                true
        );

        assertThat(money.fixedCurrencyCode()).isEqualTo("USD");
        assertThat(money.defaultCurrencyCode()).isEqualTo("EUR");
        assertThat(money.baseCurrencyCode()).isEqualTo("CNY");
        assertThat(money.rateTypeCode()).isEqualTo("SPOT");
    }

    @Test
    void shouldRejectFixedMoneyCompanionWhenNotText() {
        EntityDefinition entity = new EntityDefinition("order", "sales_order", "Order",
                java.util.List.of(
                        FieldDefinition.decimal("amount", "Amount").precision(18, 2)
                                .money(new FieldMoneyDefinition(
                                        FieldMoneyMode.FIXED,
                                        "USD",
                                        null,
                                        "amountCurrency",
                                        "amountBase",
                                        null,
                                        "SPOT",
                                        null,
                                        null,
                                        true
                                )),
                        FieldDefinition.integer("amountCurrency", "Currency").column("amount_currency"),
                        FieldDefinition.decimal("amountBase", "Base Amount").column("amount_base").precision(18, 2)
                ));

        assertThatThrownBy(() -> new ModuleDefinitionValidator().validateEntity(entity))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("currency companion field must be text");
    }

    private static final class StaticOrder {
        @MoneyField(
                defaultCurrencyCode = "usd",
                currencyFieldName = "amountCurrency",
                baseAmountFieldName = "amountBase",
                baseCurrencyCode = "cny",
                rateTypeCode = "spot",
                rateDateFieldName = "orderDate",
                exchangeRateFieldName = "amountRate"
        )
        private BigDecimal amount;
        private String amountCurrency;
        private BigDecimal amountBase;
        private LocalDate orderDate;
        private BigDecimal amountRate;

        @MoneyField(
                currencyMode = MoneyField.Mode.FIXED,
                fixedCurrencyCode = "eur",
                baseAmountFieldName = "feeBase",
                rateTypeCode = "booking",
                currencyRequired = false
        )
        private BigDecimal fee;
        private BigDecimal feeBase;
    }

    private static final class InvalidSelectableMoney {
        @MoneyField(
                baseAmountFieldName = "amountBase",
                rateTypeCode = "SPOT"
        )
        private BigDecimal amount;
    }

    @Table(name = "sales_order", comment = "Sales order")
    private static final class StaticOrderEntity extends StandardEntity {
        @MoneyField(
                defaultCurrencyCode = "USD",
                currencyFieldName = "amountCurrency",
                baseAmountFieldName = "amountBase",
                baseCurrencyCode = "CNY",
                rateTypeCode = "SPOT",
                rateDateFieldName = "orderDate",
                exchangeRateFieldName = "amountRate"
        )
        @Column(name = "amount", type = ColumnType.NUMERIC, precision = 18, scale = 2)
        private BigDecimal amount;

        @Column(name = "amount_currency", type = ColumnType.VARCHAR, length = 3)
        private String amountCurrency;

        @Column(name = "amount_base", type = ColumnType.NUMERIC, precision = 18, scale = 2)
        private BigDecimal amountBase;

        @Column(name = "order_date", type = ColumnType.DATE)
        private LocalDate orderDate;

        @Column(name = "amount_rate", type = ColumnType.NUMERIC, precision = 24, scale = 12)
        private BigDecimal amountRate;
    }

    @Table(name = "sales_sorted_order", comment = "Sorted order")
    @SortPartitionBy(fields = "customerId")
    private static final class StaticSortedOrderEntity extends StandardSortableEntity {
        @Column(name = "customer_id", type = ColumnType.VARCHAR, length = 64)
        private String customerId;
    }
}
