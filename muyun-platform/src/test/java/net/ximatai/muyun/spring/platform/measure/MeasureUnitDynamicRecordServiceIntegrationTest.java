package net.ximatai.muyun.spring.platform.measure;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.spring.common.platform.AllowAllActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.AllowAllDataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldMeasureUnitConversionMode;
import net.ximatai.muyun.spring.dynamic.metadata.FieldMeasureUnitDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldMeasureUnitMode;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicFieldValueValidator;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicModuleRegistry;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordRuntime;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
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

class MeasureUnitDynamicRecordServiceIntegrationTest {
    private static final String SCHEMA = "public";
    private static final String MODULE = "sales.order";

    private final MeasureUnitCategoryService categoryService = new MeasureUnitCategoryService(new TestMemoryDao<>());
    private final MeasureUnitService unitService = new MeasureUnitService(new TestMemoryDao<>(), categoryService);
    private final MeasureUnitConversionRuleService ruleService =
            new MeasureUnitConversionRuleService(new TestMemoryDao<>(), unitService);
    private final MeasureUnitDynamicRecordMutationCoordinator coordinator =
            new MeasureUnitDynamicRecordMutationCoordinator(
                    new MeasureUnitConversionService(categoryService, unitService),
                    new MeasureUnitBusinessConversionService(unitService, ruleService),
                    Clock.fixed(Instant.parse("2026-06-16T00:00:00Z"), ZoneOffset.UTC)
            );

    @Test
    @SuppressWarnings("unchecked")
    void shouldPersistNormalizedMeasureUnitFieldsThroughDynamicCreateService() {
        preparePackageUnits();
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), eq("sales_order_line"), anyMap(), eq(StandardEntitySchema.ID_COLUMN)))
                .thenReturn("line-1");
        DynamicRecordService service = service(operations);
        DynamicRecord record = service.newRecord(MODULE, "line")
                .setValue("quantity", new BigDecimal("2"))
                .setValue("quantityUnit", "box");

        String id = service.create(MODULE, "line", record);

        assertThat(id).isEqualTo("line-1");
        ArgumentCaptor<Map<String, Object>> body = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(operations)
                .insertItem(eq(SCHEMA), eq("sales_order_line"), body.capture(), eq(StandardEntitySchema.ID_COLUMN));
        assertThat(body.getValue())
                .containsEntry("quantity", new BigDecimal("2"))
                .containsEntry("quantity_unit", "box");
        assertThat((BigDecimal) body.getValue().get("quantity_base")).isEqualByComparingTo("24");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldPersistNormalizedMeasureUnitFieldsThroughDynamicUpdateService() {
        preparePackageUnits();
        IDatabaseOperations<Object> operations = operations();
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(beforeRow()));
        when(operations.patchUpdateItemWhere(eq(SCHEMA), eq("sales_order_line"), anyMap(), anyMap(),
                eq(StandardEntitySchema.ID_COLUMN))).thenReturn(1);
        DynamicRecordService service = service(operations);
        DynamicRecord record = service.newRecord(MODULE, "line")
                .setValue("quantity", new BigDecimal("3"));
        record.setId("line-1");

        int updated = service.update(MODULE, "line", record);

        assertThat(updated).isEqualTo(1);
        ArgumentCaptor<Map<String, Object>> body = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, Object>> where = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(operations)
                .patchUpdateItemWhere(eq(SCHEMA), eq("sales_order_line"), body.capture(), where.capture(),
                        eq(StandardEntitySchema.ID_COLUMN));
        assertThat(body.getValue())
                .containsEntry("quantity", new BigDecimal("3"));
        assertThat((BigDecimal) body.getValue().get("quantity_base")).isEqualByComparingTo("36");
        assertThat(where.getValue())
                .containsEntry(StandardEntitySchema.ID_COLUMN, "line-1")
                .containsEntry(StandardEntitySchema.VERSION_COLUMN, 1);
    }

    private DynamicRecordService service(IDatabaseOperations<Object> operations) {
        DynamicRecordRuntime runtime = DynamicRecordRuntime.builder(operations)
                .registry(new DynamicModuleRegistry())
                .fieldValueValidator(DynamicFieldValueValidator.NONE)
                .build()
                .register(new ModuleDefinition(MODULE, "Order", List.of(lineEntity())));
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

    private EntityDefinition lineEntity() {
        return new EntityDefinition(
                "line",
                "sales_order_line",
                "Line",
                List.of(
                        FieldDefinition.decimal("quantity", "Quantity").measureUnit(new FieldMeasureUnitDefinition(
                                "package",
                                FieldMeasureUnitMode.SELECTABLE,
                                null,
                                null,
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

    private Map<String, Object> beforeRow() {
        return Map.ofEntries(
                Map.entry(StandardEntitySchema.ID_COLUMN, "line-1"),
                Map.entry(StandardEntitySchema.VERSION_COLUMN, 1),
                Map.entry(StandardEntitySchema.DELETED_COLUMN, Boolean.FALSE),
                Map.entry("quantity", new BigDecimal("2")),
                Map.entry("quantity_unit", "box"),
                Map.entry("quantity_base", new BigDecimal("24"))
        );
    }

    private void preparePackageUnits() {
        MeasureUnitCategory category = new MeasureUnitCategory();
        category.setApplicationAlias("sales");
        category.setAlias("package");
        category.setDimension(MeasureDimension.COUNT);
        category.setBaseUnitCode("bottle");
        categoryService.insert(category);

        unit("bottle", BigDecimal.ONE);
        unit("box", new BigDecimal("12"));
    }

    private void unit(String code, BigDecimal factorToBase) {
        MeasureUnit unit = new MeasureUnit();
        unit.setApplicationAlias("sales");
        unit.setCategoryAlias("package");
        unit.setCode(code);
        unit.setTitle(code);
        unit.setFactorToBase(factorToBase);
        unitService.insert(unit);
    }
}
