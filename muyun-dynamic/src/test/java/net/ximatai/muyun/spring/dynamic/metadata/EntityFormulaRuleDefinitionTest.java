package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.common.formula.FormulaIssueLevel;
import net.ximatai.muyun.spring.common.formula.FormulaRuleKind;
import net.ximatai.muyun.spring.common.formula.FormulaRulePhase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityFormulaRuleDefinitionTest {
    private final ModuleDefinitionValidator validator = new ModuleDefinitionValidator();

    @Test
    void shouldConvertDynamicFormulaRuleToRuntimeRule() {
        EntityFormulaRuleDefinition rule = EntityFormulaRuleDefinition
                .validation("checkAmount", "amount", "{amount} >= 0", "金额不能小于 0")
                .phase(FormulaRulePhase.IMPORT_VALIDATE)
                .severity(FormulaIssueLevel.WARNING)
                .sortOrder(20);

        assertThat(rule.toRuntimeRule()).satisfies(runtime -> {
            assertThat(runtime.id()).isEqualTo("checkAmount");
            assertThat(runtime.expression()).isEqualTo("{amount} >= 0");
            assertThat(runtime.kind()).isEqualTo(FormulaRuleKind.VALIDATION);
            assertThat(runtime.phase()).isEqualTo(FormulaRulePhase.IMPORT_VALIDATE);
            assertThat(runtime.targetField()).isEqualTo("amount");
            assertThat(runtime.severity()).isEqualTo(FormulaIssueLevel.WARNING);
            assertThat(runtime.messageTemplate()).isEqualTo("金额不能小于 0");
            assertThat(runtime.enabled()).isTrue();
        });
    }

    @Test
    void shouldValidateFormulaRuleShapeWithEntityFields() {
        EntityDefinition entity = invoiceEntity()
                .withFormulaRules(
                        EntityFormulaRuleDefinition.calculation("amountCalc", "amount", "{quantity} * {price}"),
                        EntityFormulaRuleDefinition.calculation("lineAmountCalc", "items.lineAmount", "{items.quantity} * {items.price}")
                );

        validator.validateEntity(entity);
    }

    @Test
    void shouldValidateChildFormulaTargetAgainstModuleRelation() {
        ModuleDefinition module = new ModuleDefinition(
                "sales.invoice",
                "Invoice",
                List.of(
                        invoiceEntity().withFormulaRules(
                                EntityFormulaRuleDefinition.calculation("lineAmountCalc",
                                        "items.lineAmount", "{items.quantity} * {items.price}")
                        ),
                        invoiceLineEntity()
                ),
                List.of(EntityRelationDefinition.child("items", "invoice", "invoice_line", "invoiceId"))
        );

        validator.validate(module);
    }

    @Test
    void shouldRejectDuplicateFormulaRuleCode() {
        EntityDefinition entity = invoiceEntity()
                .withFormulaRules(
                        EntityFormulaRuleDefinition.calculation("amountCalc", "amount", "{quantity} * {price}"),
                        EntityFormulaRuleDefinition.calculation("amountCalc", "amount", "{quantity} + {price}")
                );

        assertThatThrownBy(() -> validator.validateEntity(entity))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("duplicate formula rule code: amountCalc");
    }

    @Test
    void shouldRejectUnknownMainTargetField() {
        EntityDefinition entity = invoiceEntity()
                .withFormulaRules(EntityFormulaRuleDefinition.calculation("amountCalc", "totalAmount", "{quantity} * {price}"));

        assertThatThrownBy(() -> validator.validateEntity(entity))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("unknown formula target field: invoice.totalAmount");
    }

    @Test
    void shouldRejectInvalidChildTargetPath() {
        EntityDefinition entity = invoiceEntity()
                .withFormulaRules(EntityFormulaRuleDefinition.calculation("amountCalc",
                        "items.sub.lineAmount", "{quantity} * {price}"));

        assertThatThrownBy(() -> validator.validateEntity(entity))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("invalid formula target field: items.sub.lineAmount");
    }

    @Test
    void shouldRejectInvalidFormulaExpressionDuringModuleValidation() {
        EntityDefinition entity = invoiceEntity()
                .withFormulaRules(EntityFormulaRuleDefinition.calculation("amountCalc", "amount", "SUM({quantity}"));

        assertThatThrownBy(() -> validator.validateEntity(entity))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("invalid formula expression: amountCalc");
    }

    @Test
    void shouldRejectJsonFormulaContentWithBlankExpression() {
        EntityDefinition entity = invoiceEntity()
                .withFormulaRules(EntityFormulaRuleDefinition.calculation("amountCalc", "amount", "{\"expression\":\"\"}"));

        assertThatThrownBy(() -> validator.validateEntity(entity))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("invalid formula expression: amountCalc");
    }

    @Test
    void shouldRejectUnknownChildFormulaTargetField() {
        ModuleDefinition module = new ModuleDefinition(
                "sales.invoice",
                "Invoice",
                List.of(
                        invoiceEntity().withFormulaRules(
                                EntityFormulaRuleDefinition.calculation("lineAmountCalc",
                                        "items.missingAmount", "{items.quantity} * {items.price}")
                        ),
                        invoiceLineEntity()
                ),
                List.of(EntityRelationDefinition.child("items", "invoice", "invoice_line", "invoiceId"))
        );

        assertThatThrownBy(() -> validator.validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("unknown formula target field: invoice_line.missingAmount");
    }

    @Test
    void shouldRejectUnknownChildFormulaTargetRelation() {
        ModuleDefinition module = new ModuleDefinition(
                "sales.invoice",
                "Invoice",
                List.of(
                        invoiceEntity().withFormulaRules(
                                EntityFormulaRuleDefinition.calculation("lineAmountCalc",
                                        "details.lineAmount", "{details.quantity} * {details.price}")
                        ),
                        invoiceLineEntity()
                ),
                List.of(EntityRelationDefinition.child("items", "invoice", "invoice_line", "invoiceId"))
        );

        assertThatThrownBy(() -> validator.validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("unknown formula target relation: invoice.details");
    }

    private EntityDefinition invoiceEntity() {
        return new EntityDefinition("invoice", "sales_invoice", "Invoice", List.of(
                FieldDefinition.decimal("quantity", "Quantity").precision(18, 2),
                FieldDefinition.decimal("price", "Price").precision(18, 2),
                FieldDefinition.decimal("amount", "Amount").precision(18, 2)
        ));
    }

    private EntityDefinition invoiceLineEntity() {
        return new EntityDefinition("invoice_line", "sales_invoice_line", "Invoice Line", List.of(
                FieldDefinition.string("invoiceId", "Invoice").column("invoice_id"),
                FieldDefinition.decimal("quantity", "Quantity").precision(18, 2),
                FieldDefinition.decimal("price", "Price").precision(18, 2),
                FieldDefinition.decimal("lineAmount", "Line Amount").column("line_amount").precision(18, 2)
        ));
    }
}
