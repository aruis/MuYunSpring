package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.common.formula.FormulaFieldDefinition;
import net.ximatai.muyun.spring.common.formula.FormulaValueType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicFormulaFieldDefinitionsTest {
    @Test
    void shouldMapMainEntityFieldsToFormulaDefinitions() {
        EntityDefinition entity = new EntityDefinition("contract", "contract", "Contract", List.of(
                FieldDefinition.string("code", "Code").required(),
                FieldDefinition.decimal("amount", "Amount"),
                FieldDefinition.timestamp("signedAt", "Signed At").writeProtected()
        ));

        List<FormulaFieldDefinition> fields = DynamicFormulaFieldDefinitions.mainFields(entity);

        assertThat(fields).extracting(field -> field.fieldPath().dataIndex())
                .containsExactly("code", "amount", "signedAt");
        assertThat(fields).extracting(FormulaFieldDefinition::type)
                .containsExactly(FormulaValueType.STRING, FormulaValueType.DECIMAL, FormulaValueType.TIMESTAMP);
        assertThat(fields).extracting(FormulaFieldDefinition::required)
                .containsExactly(true, false, false);
        assertThat(fields).extracting(FormulaFieldDefinition::writable)
                .containsExactly(true, true, true);
    }

    @Test
    void shouldQualifyChildEntityFieldsByRelationCode() {
        EntityDefinition entity = new EntityDefinition("items", "contract_item", "Contract Item", List.of(
                FieldDefinition.integer("qty", "Qty"),
                FieldDefinition.decimal("price", "Price")
        ));

        assertThat(DynamicFormulaFieldDefinitions.childFields("lines", entity))
                .extracting(field -> field.fieldPath().dataIndex())
                .containsExactly("lines.qty", "lines.price");
    }
}
