package net.ximatai.muyun.spring.common.option;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptionBindingTest {
    @Test
    void shouldExposeDictionarySourceAsTypedValue() {
        OptionBinding.DictionarySource source = OptionBinding.dictionary("crm", "customer_status")
                .dictionarySource();

        assertThat(source.applicationAlias()).isEqualTo("crm");
        assertThat(source.categoryAlias()).isEqualTo("customer_status");
    }

    @Test
    void shouldRejectDictionarySourceOnOtherBindingTypes() {
        assertThatThrownBy(() -> OptionBinding.enumType(State.class).dictionarySource())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not dictionary");
    }

    private enum State implements net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum {
        DRAFT;

        @Override
        public String getCode() {
            return "draft";
        }

        @Override
        public String getTitle() {
            return "Draft";
        }
    }
}
