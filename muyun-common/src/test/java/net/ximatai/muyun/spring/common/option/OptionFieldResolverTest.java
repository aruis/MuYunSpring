package net.ximatai.muyun.spring.common.option;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptionFieldResolverTest {
    @Test
    void shouldResolveDictionaryField() {
        OptionFieldDefinition definition = OptionFieldResolver.resolve(Customer.class).getFirst();

        assertThat(definition.fieldName()).isEqualTo("status");
        assertThat(definition.binding()).isEqualTo(OptionBinding.dictionary("crm", "customer_status"));
        assertThat(definition.selectionMode()).isEqualTo(OptionSelectionMode.SINGLE);
        assertThat(OptionLoadResolver.resolve(Customer.class))
                .extracting(OptionLoadDefinition::sourceField, OptionLoadDefinition::outputField,
                        OptionLoadDefinition::optionItemField)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("status", "statusTitle", "title"));
    }

    @Test
    void shouldResolveDedicatedDictionaryFieldWithBaseline() {
        OptionFieldDefinition definition = OptionFieldResolver.resolve(DictionaryCustomer.class).getFirst();

        assertThat(definition.binding()).isEqualTo(OptionBinding.dictionary("crm", "customer_level"));
        DictionaryFieldDefinition dictionary = DictionaryFieldResolver.resolve(DictionaryCustomer.class).getFirst();
        assertThat(dictionary.title()).isEqualTo("客户等级");
        assertThat(dictionary.initialItems())
                .extracting(DictionaryInitialItemDefinition::code, DictionaryInitialItemDefinition::title)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("normal", "普通客户"),
                        org.assertj.core.groups.Tuple.tuple("vip", "VIP 客户"));
    }

    @Test
    void shouldRejectBothGenericAndDedicatedOptionAnnotations() throws NoSuchFieldException {
        assertThatThrownBy(() -> OptionFieldResolver.resolve(ConflictingOptionAnnotations.class.getDeclaredField("status")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("both OptionField and DictionaryField");
    }

    @Test
    void shouldResolveEnumOptionField() {
        OptionFieldDefinition definition = OptionFieldResolver.resolve(Order.class).getFirst();

        assertThat(definition.fieldName()).isEqualTo("state");
        assertThat(definition.binding()).isEqualTo(OptionBinding.enumType(OrderState.class));
        assertThat(OptionLoadResolver.resolve(Order.class)).extracting(OptionLoadDefinition::outputField)
                .containsExactly("stateLabel");
    }

    @Test
    void shouldInferEnumOptionFieldFromEnumType() {
        OptionFieldDefinition definition = OptionFieldResolver.resolve(TypedOrder.class).getFirst();

        assertThat(definition.fieldName()).isEqualTo("state");
        assertThat(definition.binding()).isEqualTo(OptionBinding.enumType(OrderState.class));
    }

    @Test
    void shouldInferEnumOptionFieldFromCollectionElementType() {
        OptionFieldDefinition definition = OptionFieldResolver.resolve(OrderKinds.class).getFirst();

        assertThat(definition.fieldName()).isEqualTo("states");
        assertThat(definition.binding()).isEqualTo(OptionBinding.enumType(OrderState.class));
        assertThat(definition.selectionMode()).isEqualTo(OptionSelectionMode.MULTIPLE);
    }

    @Test
    void shouldInferEnumOptionFieldFromArrayElementType() {
        OptionFieldDefinition definition = OptionFieldResolver.resolve(OrderStateArray.class).getFirst();

        assertThat(definition.fieldName()).isEqualTo("states");
        assertThat(definition.binding()).isEqualTo(OptionBinding.enumType(OrderState.class));
        assertThat(definition.selectionMode()).isEqualTo(OptionSelectionMode.MULTIPLE);
    }

    @Test
    void shouldResolveStringEnumOptionFieldWithExplicitEnumType() {
        OptionFieldDefinition definition = OptionFieldResolver.resolve(StringOrder.class).getFirst();

        assertThat(definition.fieldName()).isEqualTo("state");
        assertThat(definition.binding()).isEqualTo(OptionBinding.enumType(OrderState.class));
    }

    @Test
    void shouldRejectInvalidDictionaryFieldSource() throws NoSuchFieldException {
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
    void shouldRejectOptionLoadWithoutOptionSource() {
        assertThatThrownBy(() -> OptionLoadResolver.resolve(MissingOptionSource.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("option load source is not an option field");
    }

    @Test
    void shouldRejectUnknownOptionLoadField() {
        assertThatThrownBy(() -> OptionLoadResolver.resolve(UnknownOptionItemField.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown option item field");
    }

    @Test
    void shouldRejectOptionLoadWithIncompatibleOutputType() {
        assertThatThrownBy(() -> OptionLoadResolver.resolve(IncompatibleOptionLoad.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("output field type does not accept enabled");
    }

    private static class Customer {
        @DictionaryField(source = "crm.customer_status")
        private String status;

        @OptionLoad(source = "status")
        private String statusTitle;
    }

    private static class DictionaryCustomer {
        @DictionaryField(
                source = "crm.customer_level",
                title = "客户等级",
                initialItems = {
                        @DictionaryField.InitialItem(code = "normal", title = "普通客户", sortOrder = 10),
                        @DictionaryField.InitialItem(code = "vip", title = "VIP 客户", sortOrder = 20)
                }
        )
        private String level;

        @OptionLoad(source = "level")
        private String levelTitle;
    }

    private static class ConflictingOptionAnnotations {
        @OptionField(type = OptionSourceType.ENUM, enumType = OrderState.class)
        @DictionaryField(source = "crm.status")
        private String status;

        private String statusTitle;
    }

    private static class Order {
        @OptionField(type = OptionSourceType.ENUM, enumType = OrderState.class)
        private String state;

        @OptionLoad(source = "state")
        private String stateLabel;
    }

    private static class TypedOrder {
        @OptionField(type = OptionSourceType.ENUM)
        private OrderState state;

        @OptionLoad(source = "state")
        private String stateTitle;
    }

    private static class OrderKinds {
        @OptionField(type = OptionSourceType.ENUM, selectionMode = OptionSelectionMode.MULTIPLE)
        private List<OrderState> states;

        @OptionLoad(source = "states")
        private List<String> statesTitle;
    }

    private static class OrderStateArray {
        @OptionField(type = OptionSourceType.ENUM, selectionMode = OptionSelectionMode.MULTIPLE)
        private OrderState[] states;

        @OptionLoad(source = "states")
        private List<String> statesTitle;
    }

    private static class StringOrder {
        @OptionField(type = OptionSourceType.ENUM, enumType = OrderState.class)
        private String state;

        private String stateTitle;
    }

    private static class InvalidDictionary {
        @DictionaryField(source = "customer_status")
        private String status;
    }

    private static class InvalidEnumSource {
        @OptionField(type = OptionSourceType.ENUM, source = "legacy.Source")
        private OrderState state;
    }

    private static class MissingEnumType {
        @OptionField(type = OptionSourceType.ENUM)
        private String state;
    }

    private static class CollectionModeMismatch {
        @OptionField(type = OptionSourceType.ENUM)
        private List<OrderState> states;
    }

    private static class ScalarModeMismatch {
        @OptionField(type = OptionSourceType.ENUM, selectionMode = OptionSelectionMode.MULTIPLE)
        private OrderState state;
    }

    private static class MissingOptionSource {
        @OptionLoad(source = "status")
        private String statusTitle;
    }

    private static class UnknownOptionItemField {
        @DictionaryField(source = "crm.customer_status")
        private String status;

        @OptionLoad(source = "status", field = "unknown")
        private String statusUnknown;
    }

    private static class IncompatibleOptionLoad {
        @DictionaryField(source = "crm.customer_status")
        private String status;

        @OptionLoad(source = "status", field = "enabled")
        private String statusEnabled;
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
