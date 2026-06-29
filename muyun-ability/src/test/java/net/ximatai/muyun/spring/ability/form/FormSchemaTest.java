package net.ximatai.muyun.spring.ability.form;

import net.ximatai.muyun.spring.common.option.OptionBinding;
import net.ximatai.muyun.spring.common.option.OptionField;
import net.ximatai.muyun.spring.common.option.OptionSourceType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FormSchemaTest {
    @Test
    void shouldExposeDescriptorAsFrontendConsumableSchema() {
        FormDescriptor descriptor = FormDescriptor.builder("iam.employee")
                .title("职员档案")
                .field(FormField.of("employeeNo").withTitle("职员编号").asRequired())
                .field(FormField.of("enabled", FormValueType.BOOLEAN).withTitle("启用状态"))
                .build();

        FormSchema schema = FormSchema.from(descriptor);

        assertThat(schema.scopeName()).isEqualTo("iam.employee");
        assertThat(schema.title()).isEqualTo("职员档案");
        assertThat(schema.fields()).hasSize(2);
        assertThat(schema.fields().getFirst()).satisfies(field -> {
            assertThat(field.name()).isEqualTo("employeeNo");
            assertThat(field.title()).isEqualTo("职员编号");
            assertThat(field.valueType()).isEqualTo(FormValueType.STRING);
            assertThat(field.controlType()).isEqualTo(FormControlType.TEXT);
            assertThat(field.required()).isTrue();
        });
        assertThat(schema.fields().get(1)).satisfies(field -> {
            assertThat(field.name()).isEqualTo("enabled");
            assertThat(field.controlType()).isEqualTo(FormControlType.SWITCH);
        });
    }

    @Test
    void shouldMergeStaticOptionFieldMetadataIntoFormSchema() {
        FormDescriptor descriptor = FormDescriptor.builder("iam.employee")
                .field(FormField.of("gender").withTitle("性别"))
                .build();

        FormSchema schema = FormSchema.from(descriptor, EmployeeOptionRecord.class);

        assertThat(schema.fields()).singleElement().satisfies(field -> {
            assertThat(field.name()).isEqualTo("gender");
            assertThat(field.optionBinding()).isEqualTo(OptionBinding.dictionary("iam", "gender"));
            assertThat(field.controlType()).isEqualTo(FormControlType.SELECT);
            assertThat(field.optionTitleField()).isEqualTo("genderTitle");
        });
    }

    @Test
    void shouldKeepExplicitFormOptionBindingWhenStaticOptionFieldAlsoExists() {
        FormDescriptor descriptor = FormDescriptor.builder("iam.employee")
                .field(FormField.of("gender")
                        .withOptionBinding(OptionBinding.dictionary("crm", "gender")))
                .build();

        FormSchema schema = FormSchema.from(descriptor, EmployeeOptionRecord.class);

        assertThat(schema.fields()).singleElement()
                .extracting(FormSchema.Field::optionBinding)
                .isEqualTo(OptionBinding.dictionary("crm", "gender"));
    }

    private static class EmployeeOptionRecord {
        @OptionField(type = OptionSourceType.DICTIONARY, source = "iam.gender")
        private String gender;

        private String genderTitle;
    }
}
