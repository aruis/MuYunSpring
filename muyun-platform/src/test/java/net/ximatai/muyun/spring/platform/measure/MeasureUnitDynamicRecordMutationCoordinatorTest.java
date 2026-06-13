package net.ximatai.muyun.spring.platform.measure;

import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldMeasureUnitConversionMode;
import net.ximatai.muyun.spring.dynamic.metadata.FieldMeasureUnitDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldMeasureUnitMode;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MeasureUnitDynamicRecordMutationCoordinatorTest {
    private final MeasureUnitCategoryService categoryService = new MeasureUnitCategoryService(new TestMemoryDao<>());
    private final MeasureUnitService unitService = new MeasureUnitService(new TestMemoryDao<>(), categoryService);
    private final MeasureUnitConversionRuleService ruleService =
            new MeasureUnitConversionRuleService(new TestMemoryDao<>(), unitService);
    private final MeasureUnitDynamicRecordMutationCoordinator coordinator =
            new MeasureUnitDynamicRecordMutationCoordinator(
                    new MeasureUnitConversionService(categoryService, unitService),
                    new MeasureUnitBusinessConversionService(unitService, ruleService),
                    Clock.fixed(Instant.parse("2026-06-13T00:00:00Z"), ZoneOffset.UTC)
            );

    @Test
    void shouldNormalizeSelectableMeasureUnitBeforeDynamicCreate() {
        packageUnits();
        DynamicRecord record = new DynamicRecord(packageEntity())
                .setValue("quantity", new BigDecimal("2"))
                .setValue("quantityUnit", "box");

        coordinator.beforeCreate("sales.order", "line", record);

        assertThat(baseValue(record)).isEqualByComparingTo("24");
    }

    @Test
    void shouldReuseExistingUnitWhenOnlyMeasureValueChangesOnUpdate() {
        packageUnits();
        DynamicRecord before = new DynamicRecord(packageEntity())
                .setValue("quantity", new BigDecimal("2"))
                .setValue("quantityUnit", "box");
        coordinator.beforeCreate("sales.order", "line", before);
        DynamicRecord incoming = new DynamicRecord(packageEntity())
                .setValue("quantity", new BigDecimal("3"));

        coordinator.beforeUpdate("sales.order", "line", before, incoming);

        assertThat(baseValue(incoming)).isEqualByComparingTo("36");
    }

    @Test
    void shouldReuseExistingValueWhenOnlyMeasureUnitChangesOnUpdate() {
        packageUnits();
        DynamicRecord before = new DynamicRecord(packageEntity())
                .setValue("quantity", new BigDecimal("24"))
                .setValue("quantityUnit", "bottle");
        coordinator.beforeCreate("sales.order", "line", before);
        DynamicRecord incoming = new DynamicRecord(packageEntity())
                .setValue("quantityUnit", "box");

        coordinator.beforeUpdate("sales.order", "line", before, incoming);

        assertThat(baseValue(incoming)).isEqualByComparingTo("288");
    }

    @Test
    void shouldApplyDefaultSelectableUnitBeforeDynamicCreate() {
        packageUnits();
        DynamicRecord record = new DynamicRecord(packageEntityWithDefaultUnit())
                .setValue("quantity", new BigDecimal("2"));

        coordinator.beforeCreate("sales.order", "line", record);

        assertThat(record.getValue("quantityUnit")).isEqualTo("box");
        assertThat(baseValue(record)).isEqualByComparingTo("24");
    }

    @Test
    void shouldBackfillDefaultSelectableUnitWhenUpdatingOldRecordWithoutUnit() {
        packageUnits();
        DynamicRecord before = new DynamicRecord(packageEntityWithDefaultUnit())
                .setValue("quantity", new BigDecimal("1"));
        DynamicRecord incoming = new DynamicRecord(packageEntityWithDefaultUnit())
                .setValue("quantity", new BigDecimal("2"));

        coordinator.beforeUpdate("sales.order", "line", before, incoming);

        assertThat(incoming.getValue("quantityUnit")).isEqualTo("box");
        assertThat(baseValue(incoming)).isEqualByComparingTo("24");
    }

    @Test
    void shouldRejectExplicitlyClearedUnitWhenMeasureValueExists() {
        packageUnits();
        DynamicRecord before = new DynamicRecord(packageEntity())
                .setValue("quantity", new BigDecimal("2"))
                .setValue("quantityUnit", "box");
        DynamicRecord incoming = new DynamicRecord(packageEntity())
                .setValue("quantityUnit", null);

        assertThatThrownBy(() -> coordinator.beforeUpdate("sales.order", "line", before, incoming))
                .hasMessageContaining("quantityUnit");
    }

    @Test
    void shouldApplyBusinessRuleForCrossCategoryBaseUnit() {
        rollAndLengthUnits();
        MeasureUnitConversionRule rule = new MeasureUnitConversionRule();
        rule.setApplicationAlias("sales");
        rule.setScopeType(MeasureUnitConversionScopeType.GLOBAL);
        rule.setFromCategoryAlias("roll");
        rule.setFromUnitCode("roll");
        rule.setToCategoryAlias("length");
        rule.setToUnitCode("m");
        rule.setFactor(new BigDecimal("30"));
        ruleService.insert(rule);
        DynamicRecord record = new DynamicRecord(rollEntity())
                .setValue("quantity", new BigDecimal("2"));

        coordinator.beforeCreate("sales.order", "line", record);

        assertThat(baseValue(record)).isEqualByComparingTo("60");
    }

    @Test
    void shouldReuseExistingConversionScopeWhenOnlyMeasureValueChangesOnUpdate() {
        rollAndLengthUnits();
        MeasureUnitConversionRule rule = new MeasureUnitConversionRule();
        rule.setApplicationAlias("sales");
        rule.setScopeType(MeasureUnitConversionScopeType.RECORD_CONTEXT);
        rule.setModuleAlias("sales.order");
        rule.setContextObjectType("sku_id");
        rule.setContextObjectId("sku-1");
        rule.setFromCategoryAlias("roll");
        rule.setFromUnitCode("roll");
        rule.setToCategoryAlias("length");
        rule.setToUnitCode("m");
        rule.setFactor(new BigDecimal("30"));
        ruleService.insert(rule);
        DynamicRecord before = new DynamicRecord(rollEntityWithScope())
                .setValue("quantity", new BigDecimal("1"))
                .setValue("skuId", "sku-1");
        DynamicRecord incoming = new DynamicRecord(rollEntityWithScope())
                .setValue("quantity", new BigDecimal("3"));

        coordinator.beforeUpdate("sales.order", "line", before, incoming);

        assertThat(baseValue(incoming)).isEqualByComparingTo("90");
    }

    private EntityDefinition packageEntity() {
        return packageEntity(null);
    }

    private EntityDefinition packageEntityWithDefaultUnit() {
        return packageEntity("box");
    }

    private EntityDefinition packageEntity(String defaultUnitCode) {
        return new EntityDefinition(
                "line",
                "sales_order_line",
                "Line",
                List.of(
                        FieldDefinition.decimal("quantity", "Quantity").measureUnit(new FieldMeasureUnitDefinition(
                                "package",
                                FieldMeasureUnitMode.SELECTABLE,
                                null,
                                defaultUnitCode,
                                "quantityUnit",
                                "quantityBase",
                                "package",
                                "bottle",
                                FieldMeasureUnitConversionMode.LINEAR,
                                null,
                                true
                        )),
                        FieldDefinition.string("quantityUnit", "Unit").column("quantity_unit").length(64),
                        FieldDefinition.decimal("quantityBase", "Base Quantity").column("quantity_base")
                ),
                Set.of(EntityCapability.CRUD)
        );
    }

    private EntityDefinition rollEntity() {
        return rollEntity(null);
    }

    private EntityDefinition rollEntityWithScope() {
        return rollEntity("skuId");
    }

    private EntityDefinition rollEntity(String conversionScopeFieldName) {
        List<FieldDefinition> fields = new java.util.ArrayList<>(List.of(
                FieldDefinition.decimal("quantity", "Quantity").measureUnit(new FieldMeasureUnitDefinition(
                        "roll",
                        FieldMeasureUnitMode.FIXED,
                        "roll",
                        "roll",
                        null,
                        "quantityBase",
                        "length",
                        "m",
                        FieldMeasureUnitConversionMode.BUSINESS_RULE,
                        conversionScopeFieldName,
                        false
                )),
                FieldDefinition.decimal("quantityBase", "Base Quantity").column("quantity_base")
        ));
        if (conversionScopeFieldName != null) {
            fields.add(FieldDefinition.string(conversionScopeFieldName, "SKU").column("sku_id").length(64));
        }
        return new EntityDefinition(
                "line",
                "sales_order_line",
                "Line",
                fields,
                Set.of(EntityCapability.CRUD)
        );
    }

    private void packageUnits() {
        category("sales", "package", "bottle", MeasureDimension.COUNT);
        unit("sales", "package", "bottle", BigDecimal.ONE);
        unit("sales", "package", "box", new BigDecimal("12"));
    }

    private void rollAndLengthUnits() {
        category("sales", "roll", "roll", MeasureDimension.COUNT);
        unit("sales", "roll", "roll", BigDecimal.ONE);
        category("sales", "length", "m", MeasureDimension.LENGTH);
        unit("sales", "length", "m", BigDecimal.ONE);
    }

    private void category(String applicationAlias, String alias, String baseUnitCode, MeasureDimension dimension) {
        MeasureUnitCategory category = new MeasureUnitCategory();
        category.setApplicationAlias(applicationAlias);
        category.setAlias(alias);
        category.setDimension(dimension);
        category.setBaseUnitCode(baseUnitCode);
        categoryService.insert(category);
    }

    private void unit(String applicationAlias, String categoryAlias, String code, BigDecimal factorToBase) {
        MeasureUnit unit = new MeasureUnit();
        unit.setApplicationAlias(applicationAlias);
        unit.setCategoryAlias(categoryAlias);
        unit.setCode(code);
        unit.setTitle(code);
        unit.setFactorToBase(factorToBase);
        unitService.insert(unit);
    }

    private BigDecimal baseValue(DynamicRecord record) {
        return (BigDecimal) record.getValue("quantityBase");
    }
}
