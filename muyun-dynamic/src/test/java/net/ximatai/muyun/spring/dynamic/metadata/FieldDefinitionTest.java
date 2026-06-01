package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.common.option.OptionBinding;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FieldDefinitionTest {
    @Test
    void shouldExposeDictionaryBindingAsUnifiedOptionBinding() {
        FieldDefinition field = FieldDefinition.string("status", "Status")
                .dictionary("crm", "customer_status");

        assertThat(field.dictionaryBinding())
                .isEqualTo(new FieldDictionaryBinding("crm", "customer_status"));
        assertThat(field.optionBinding())
                .isEqualTo(OptionBinding.dictionary("crm", "customer_status"));
    }

    @Test
    void shouldExposeQueryDefinitionOnField() {
        FieldDefinition title = FieldDefinition.titleField().queryable();
        FieldDefinition status = FieldDefinition.string("status", "Status")
                .queryable(DynamicQueryOperator.EQ, Set.of(DynamicQueryOperator.EQ, DynamicQueryOperator.IN));

        assertThat(title.queryDefinition().queryable()).isTrue();
        assertThat(title.queryDefinition().defaultOperator()).isEqualTo(DynamicQueryOperator.LIKE);
        assertThat(status.queryDefinition().operators()).containsExactlyInAnyOrder(DynamicQueryOperator.EQ, DynamicQueryOperator.IN);
    }

    @Test
    void shouldExposeFieldBehaviorDefinition() {
        FieldDefinition field = FieldDefinition.string("code", "Code")
                .defaultValue("AUTO")
                .validationRegex("[A-Z]+")
                .notCopyable()
                .writeProtected();

        assertThat(field.behavior().defaultValue()).isEqualTo("AUTO");
        assertThat(field.behavior().validationRegex()).isEqualTo("[A-Z]+");
        assertThat(field.behavior().copyable()).isFalse();
        assertThat(field.behavior().writeProtected()).isTrue();
    }

    @Test
    void shouldRejectQueryOperatorUnsupportedByFieldType() {
        assertThatThrownBy(() -> FieldDefinition.decimal("amount", "Amount")
                .queryable(DynamicQueryOperator.LIKE, Set.of(DynamicQueryOperator.LIKE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not supported");
    }

    @Test
    void shouldRejectInvalidFieldBehaviorDefinition() {
        ModuleDefinitionValidator validator = new ModuleDefinitionValidator();

        assertThatThrownBy(() -> validator.validateEntity(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                java.util.List.of(FieldDefinition.string("code", "Code")
                        .defaultValue("abc")
                        .validationRegex("[A-Z]+"))
        )))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("defaultValue");
        assertThatThrownBy(() -> validator.validateEntity(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                java.util.List.of(FieldDefinition.bool("enabled", "Enabled").defaultValue("abc"))
        )))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("boolean defaultValue");
    }
}
