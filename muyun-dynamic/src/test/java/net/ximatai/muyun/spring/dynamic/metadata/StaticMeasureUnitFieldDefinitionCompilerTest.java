package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.common.measure.MeasureUnitField;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StaticMeasureUnitFieldDefinitionCompilerTest {
    @Test
    void shouldCompileSelectableStaticMeasureUnitField() {
        FieldDefinition compiled = StaticMeasureUnitFieldDefinitionCompiler.compile(
                FieldDefinition.decimal("quantity", "Quantity").precision(18, 2),
                StaticOrderLine.class);

        assertThat(compiled.measureUnit()).satisfies(measureUnit -> {
            assertThat(measureUnit.enabled()).isTrue();
            assertThat(measureUnit.categoryAlias()).isEqualTo("quantity");
            assertThat(measureUnit.mode()).isEqualTo(FieldMeasureUnitMode.SELECTABLE);
            assertThat(measureUnit.defaultUnitCode()).isEqualTo("box");
            assertThat(measureUnit.unitFieldName()).isEqualTo("quantityUnit");
            assertThat(measureUnit.baseValueFieldName()).isEqualTo("quantityBase");
            assertThat(measureUnit.baseUnitCategoryAlias()).isEqualTo("quantity");
            assertThat(measureUnit.baseUnitCode()).isEqualTo("bottle");
            assertThat(measureUnit.conversionMode()).isEqualTo(FieldMeasureUnitConversionMode.BUSINESS_RULE);
            assertThat(measureUnit.conversionScopeFieldName()).isEqualTo("skuId");
            assertThat(measureUnit.unitRequired()).isTrue();
        });
    }

    @Test
    void shouldCompileFixedStaticMeasureUnitField() {
        FieldDefinition compiled = StaticMeasureUnitFieldDefinitionCompiler.compile(
                FieldDefinition.decimal("length", "Length").precision(18, 2),
                StaticOrderLine.class);

        assertThat(compiled.measureUnit()).satisfies(measureUnit -> {
            assertThat(measureUnit.enabled()).isTrue();
            assertThat(measureUnit.mode()).isEqualTo(FieldMeasureUnitMode.FIXED);
            assertThat(measureUnit.fixedUnitCode()).isEqualTo("meter");
            assertThat(measureUnit.unitFieldName()).isNull();
            assertThat(measureUnit.baseValueFieldName()).isEqualTo("lengthBase");
            assertThat(measureUnit.baseUnitCode()).isEqualTo("meter");
            assertThat(measureUnit.unitRequired()).isFalse();
        });
    }

    @Test
    void shouldReturnDefinitionWhenStaticFieldHasNoMeasureUnitAnnotation() {
        FieldDefinition definition = FieldDefinition.string("skuId", "SKU").length(64);

        FieldDefinition compiled = StaticMeasureUnitFieldDefinitionCompiler.compile(definition, StaticOrderLine.class);

        assertThat(compiled).isSameAs(definition);
        assertThat(compiled.measureUnit().enabled()).isFalse();
    }

    @Test
    void shouldRejectSelectableStaticMeasureUnitWithoutUnitFieldName() throws NoSuchFieldException {
        java.lang.reflect.Field field = InvalidSelectableMeasure.class.getDeclaredField("quantity");

        assertThatThrownBy(() -> StaticMeasureUnitFieldDefinitionCompiler.measureUnit(field))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("selectable unit mode requires unitFieldName");
    }

    @Test
    void shouldCompileStaticEntityDefinitionAndPassUnifiedMeasureValidation() {
        EntityDefinition entity = new StaticEntityDefinitionCompiler()
                .compile("order_line", "Order Line", StaticOrderLineEntity.class);

        new ModuleDefinitionValidator().validateEntity(entity);
        assertThat(entity.fields()).filteredOn(field -> field.fieldName().equals("quantity"))
                .singleElement()
                .satisfies(field -> assertThat(field.measureUnit().baseValueFieldName()).isEqualTo("quantityBase"));
    }

    private static final class StaticOrderLine {
        @MeasureUnitField(
                categoryAlias = "quantity",
                defaultUnitCode = "box",
                unitFieldName = "quantityUnit",
                baseValueFieldName = "quantityBase",
                baseUnitCode = "bottle",
                conversionMode = MeasureUnitField.ConversionMode.BUSINESS_RULE,
                conversionScopeFieldName = "skuId"
        )
        private BigDecimal quantity;
        private String quantityUnit;
        private BigDecimal quantityBase;
        private String skuId;

        @MeasureUnitField(
                categoryAlias = "length",
                mode = MeasureUnitField.Mode.FIXED,
                fixedUnitCode = "meter",
                baseValueFieldName = "lengthBase",
                baseUnitCode = "meter",
                unitRequired = false
        )
        private BigDecimal length;
        private BigDecimal lengthBase;
    }

    private static final class InvalidSelectableMeasure {
        @MeasureUnitField(
                categoryAlias = "quantity",
                baseValueFieldName = "quantityBase",
                baseUnitCode = "bottle"
        )
        private BigDecimal quantity;
    }

    @Table(name = "sales_order_line", comment = "Sales order line")
    private static final class StaticOrderLineEntity extends StandardEntity {
        @MeasureUnitField(
                categoryAlias = "quantity",
                defaultUnitCode = "box",
                unitFieldName = "quantityUnit",
                baseValueFieldName = "quantityBase",
                baseUnitCode = "bottle",
                conversionMode = MeasureUnitField.ConversionMode.BUSINESS_RULE,
                conversionScopeFieldName = "skuId"
        )
        @Column(name = "quantity", type = ColumnType.NUMERIC, precision = 18, scale = 2)
        private BigDecimal quantity;

        @Column(name = "quantity_unit", type = ColumnType.VARCHAR, length = 64)
        private String quantityUnit;

        @Column(name = "quantity_base", type = ColumnType.NUMERIC, precision = 18, scale = 2)
        private BigDecimal quantityBase;

        @Column(name = "sku_id", type = ColumnType.VARCHAR, length = 64)
        private String skuId;
    }
}
