package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.common.option.OptionBinding;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
