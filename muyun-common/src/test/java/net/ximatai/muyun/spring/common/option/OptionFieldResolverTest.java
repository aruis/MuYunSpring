package net.ximatai.muyun.spring.common.option;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptionFieldResolverTest {
    @Test
    void shouldResolveDictionaryOptionField() {
        OptionFieldDefinition definition = OptionFieldResolver.resolve(Customer.class).getFirst();

        assertThat(definition.fieldName()).isEqualTo("status");
        assertThat(definition.binding()).isEqualTo(OptionBinding.dictionary("crm", "customer_status"));
        assertThat(definition.selectionMode()).isEqualTo(OptionSelectionMode.SINGLE);
        assertThat(definition.titleOutput()).isEqualTo(OptionTitleOutput.AUTO);
        assertThat(definition.titleOutputField()).isEqualTo("statusTitle");
    }

    @Test
    void shouldResolveEnumOptionField() {
        OptionFieldDefinition definition = OptionFieldResolver.resolve(Order.class).getFirst();

        assertThat(definition.fieldName()).isEqualTo("state");
        assertThat(definition.binding()).isEqualTo(OptionBinding.enumType(OrderState.class));
        assertThat(definition.titleOutputField()).isEqualTo("stateLabel");
    }

    @Test
    void shouldRejectInvalidDictionarySource() throws NoSuchFieldException {
        assertThatThrownBy(() -> OptionFieldResolver.resolve(InvalidDictionary.class.getDeclaredField("status")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("applicationAlias.categoryAlias");
    }

    @Test
    void shouldRejectMissingAutoTitleOutputField() throws NoSuchFieldException {
        assertThatThrownBy(() -> OptionFieldResolver.resolve(MissingTitleOutput.class.getDeclaredField("status")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("option title output field does not exist");
    }

    private static class Customer {
        @OptionField(type = OptionSourceType.DICTIONARY, source = "crm.customer_status")
        private String status;

        private String statusTitle;
    }

    private static class Order {
        @OptionField(type = OptionSourceType.ENUM,
                source = "net.ximatai.muyun.spring.common.option.OptionFieldResolverTest$OrderState",
                titleOutput = OptionTitleOutput.CUSTOM,
                titleOutputField = "stateLabel")
        private String state;

        private String stateLabel;
    }

    private static class InvalidDictionary {
        @OptionField(type = OptionSourceType.DICTIONARY, source = "customer_status",
                titleOutput = OptionTitleOutput.NONE)
        private String status;
    }

    private static class MissingTitleOutput {
        @OptionField(type = OptionSourceType.DICTIONARY, source = "crm.customer_status")
        private String status;
    }

    private enum OrderState implements CodeTitleEnum {
        NEW("new", "New");

        private final String code;
        private final String title;

        OrderState(String code, String title) {
            this.code = code;
            this.title = title;
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getTitle() {
            return title;
        }
    }
}
