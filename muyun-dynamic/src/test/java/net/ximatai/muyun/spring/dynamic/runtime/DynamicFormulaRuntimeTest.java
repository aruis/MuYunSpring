package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.common.formula.FormulaRuleKind;
import net.ximatai.muyun.spring.common.formula.FormulaRulePhase;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityFormulaRuleDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DynamicFormulaRuntimeTest {

    @Test
    void shouldThrowWhenImportValidationFails() {
        EntityDefinition entity = orderEntity().withFormulaRules(
                EntityFormulaRuleDefinition.validation("orderNoRequired",
                                "orderNo", "{orderNo} != ''", "订单号不能为空")
                        .phase(FormulaRulePhase.IMPORT_VALIDATE)
                        .stoppingOnError()
        );
        ModuleDefinition module = new ModuleDefinition("sales.order", "Order", List.of(entity));
        DynamicFormulaRuntime runtime = new DynamicFormulaRuntime(module.moduleAlias(), entity, module);
        DynamicRecord record = new DynamicRecord(entity).setValue("orderNo", "");

        assertThatThrownBy(() -> runtime.importValidate(record, null))
                .isInstanceOf(DynamicFormulaException.class)
                .hasMessageContaining("dynamic formula rule failed: sales.order.order")
                .hasMessageContaining("orderNoRequired");
    }

    @Test
    void shouldNotApplyImportValidationCalculationSideEffects() {
        EntityDefinition recordEntity = orderEntity();
        EntityDefinition runtimeEntity = orderEntity().withFormulaRules(
                new EntityFormulaRuleDefinition("normalizeOrderNo",
                        "{normalizedOrderNo} = 'NORMALIZED'",
                        FormulaRuleKind.CALCULATION,
                        FormulaRulePhase.IMPORT_VALIDATE,
                        null)
        );
        ModuleDefinition module = new ModuleDefinition("sales.order", "Order", List.of(runtimeEntity));
        DynamicFormulaRuntime runtime = new DynamicFormulaRuntime(module.moduleAlias(), runtimeEntity, module);
        DynamicRecord record = new DynamicRecord(recordEntity).setValue("orderNo", "A-001");

        runtime.importValidate(record, null);

        assertThat(record.getValues()).doesNotContainKey("normalizedOrderNo");
    }

    @Test
    void shouldPreviewCalculatedVirtualField() {
        EntityDefinition entity = new EntityDefinition("order", "sales_order", "Order", List.of(
                FieldDefinition.string("orderNo", "Order No").column("order_no"),
                FieldDefinition.string("displayName", "Display Name").column("display_name").virtual()
        )).withFormulaRules(EntityFormulaRuleDefinition.calculation(
                "displayNameCalc", "displayName", "'Order ' + {orderNo}"));
        ModuleDefinition module = new ModuleDefinition("sales.order", "Order", List.of(entity));
        DynamicFormulaRuntime runtime = new DynamicFormulaRuntime(module.moduleAlias(), entity, module);
        DynamicRecord record = new DynamicRecord(entity).setValue("orderNo", "A-001");

        DynamicFormulaPreviewResult result = runtime.preview(record, null);

        assertThat(result.record().getValue("displayName")).isEqualTo("Order A-001");
        assertThat(result.changedFields()).containsExactly("displayName");
        assertThat(result.report().hasErrors()).isFalse();
    }

    private EntityDefinition orderEntity() {
        return new EntityDefinition("order", "sales_order", "Order", List.of(
                FieldDefinition.string("orderNo", "Order No").column("order_no"),
                FieldDefinition.string("normalizedOrderNo", "Normalized Order No").column("normalized_order_no")
        ));
    }
}
