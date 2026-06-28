package net.ximatai.muyun.spring.common.option;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    void shouldInferEnumOptionFieldFromEnumType() {
        OptionFieldDefinition definition = OptionFieldResolver.resolve(TypedOrder.class).getFirst();

        assertThat(definition.fieldName()).isEqualTo("state");
        assertThat(definition.binding()).isEqualTo(OptionBinding.enumType(OrderState.class));
        assertThat(definition.titleOutputField()).isEqualTo("stateTitle");
    }

    @Test
    void shouldInferEnumOptionFieldFromCollectionElementType() {
        OptionFieldDefinition definition = OptionFieldResolver.resolve(OrderKinds.class).getFirst();

        assertThat(definition.fieldName()).isEqualTo("states");
        assertThat(definition.binding()).isEqualTo(OptionBinding.enumType(OrderState.class));
        assertThat(definition.selectionMode()).isEqualTo(OptionSelectionMode.MULTIPLE);
        assertThat(definition.titleOutputField()).isEqualTo("statesTitle");
    }

    @Test
    void shouldInferEnumOptionFieldFromArrayElementType() {
        OptionFieldDefinition definition = OptionFieldResolver.resolve(OrderStateArray.class).getFirst();

        assertThat(definition.fieldName()).isEqualTo("states");
        assertThat(definition.binding()).isEqualTo(OptionBinding.enumType(OrderState.class));
        assertThat(definition.selectionMode()).isEqualTo(OptionSelectionMode.MULTIPLE);
        assertThat(definition.titleOutputField()).isEqualTo("statesTitle");
    }

    @Test
    void shouldResolveStringEnumOptionFieldWithExplicitEnumType() {
        OptionFieldDefinition definition = OptionFieldResolver.resolve(StringOrder.class).getFirst();

        assertThat(definition.fieldName()).isEqualTo("state");
        assertThat(definition.binding()).isEqualTo(OptionBinding.enumType(OrderState.class));
    }

    @Test
    void shouldRejectInvalidDictionarySource() throws NoSuchFieldException {
        assertThatThrownBy(() -> OptionFieldResolver.resolve(InvalidDictionary.class.getDeclaredField("status")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("applicationAlias.categoryAlias");
    }

    @Test
    void shouldRejectEnumOptionSourceString() throws NoSuchFieldException {
        assertThatThrownBy(() -> OptionFieldResolver.resolve(InvalidEnumSource.class.getDeclaredField("state")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not source");
    }

    @Test
    void shouldRejectEnumOptionFieldWithoutEnumType() throws NoSuchFieldException {
        assertThatThrownBy(() -> OptionFieldResolver.resolve(MissingEnumType.class.getDeclaredField("state")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires enumType or CodeTitleEnum field type");
    }

    @Test
    void shouldRejectCollectionOptionFieldWithoutMultipleSelectionMode() throws NoSuchFieldException {
        assertThatThrownBy(() -> OptionFieldResolver.resolve(CollectionModeMismatch.class.getDeclaredField("states")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires MULTIPLE selection mode");
    }

    @Test
    void shouldRejectScalarOptionFieldWithMultipleSelectionMode() throws NoSuchFieldException {
        assertThatThrownBy(() -> OptionFieldResolver.resolve(ScalarModeMismatch.class.getDeclaredField("state")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires collection or array field");
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
                enumType = OrderState.class,
                titleOutput = OptionTitleOutput.CUSTOM,
                titleOutputField = "stateLabel")
        private String state;

        private String stateLabel;
    }

    private static class TypedOrder {
        @OptionField(type = OptionSourceType.ENUM)
        private OrderState state;

        private String stateTitle;
    }

    private static class OrderKinds {
        @OptionField(type = OptionSourceType.ENUM, selectionMode = OptionSelectionMode.MULTIPLE)
        private List<OrderState> states;

        private List<String> statesTitle;
    }

    private static class OrderStateArray {
        @OptionField(type = OptionSourceType.ENUM, selectionMode = OptionSelectionMode.MULTIPLE)
        private OrderState[] states;

        private List<String> statesTitle;
    }

    private static class StringOrder {
        @OptionField(type = OptionSourceType.ENUM, enumType = OrderState.class)
        private String state;

        private String stateTitle;
    }

    private static class InvalidDictionary {
        @OptionField(type = OptionSourceType.DICTIONARY, source = "customer_status",
                titleOutput = OptionTitleOutput.NONE)
        private String status;
    }

    private static class InvalidEnumSource {
        @OptionField(type = OptionSourceType.ENUM, source = "legacy.Source",
                titleOutput = OptionTitleOutput.NONE)
        private OrderState state;
    }

    private static class MissingEnumType {
        @OptionField(type = OptionSourceType.ENUM, titleOutput = OptionTitleOutput.NONE)
        private String state;
    }

    private static class CollectionModeMismatch {
        @OptionField(type = OptionSourceType.ENUM, titleOutput = OptionTitleOutput.NONE)
        private List<OrderState> states;
    }

    private static class ScalarModeMismatch {
        @OptionField(type = OptionSourceType.ENUM,
                selectionMode = OptionSelectionMode.MULTIPLE,
                titleOutput = OptionTitleOutput.NONE)
        private OrderState state;
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
