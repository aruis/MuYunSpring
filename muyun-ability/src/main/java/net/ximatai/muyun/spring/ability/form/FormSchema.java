package net.ximatai.muyun.spring.ability.form;

import net.ximatai.muyun.spring.common.option.OptionBinding;
import net.ximatai.muyun.spring.common.option.OptionFieldDefinition;
import net.ximatai.muyun.spring.common.option.OptionFieldResolver;
import net.ximatai.muyun.spring.common.option.OptionSelectionMode;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record FormSchema(String scopeName,
                         String title,
                         List<Field> fields) {
    public FormSchema {
        if (scopeName == null || scopeName.isBlank()) {
            throw new IllegalArgumentException("form schema scope name must not be blank");
        }
        fields = fields == null ? List.of() : List.copyOf(fields);
    }

    public static FormSchema from(FormDescriptor descriptor) {
        return from(descriptor, null);
    }

    public static FormSchema from(FormDescriptor descriptor, Class<?> modelClass) {
        if (descriptor == null) {
            throw new IllegalArgumentException("form descriptor must not be null");
        }
        Map<String, OptionFieldDefinition> optionFields = optionFields(modelClass);
        return new FormSchema(
                descriptor.scopeName(),
                descriptor.title(),
                descriptor.fields().stream()
                        .map(field -> Field.from(mergeOptionField(field, optionFields)))
                        .toList()
        );
    }

    private static FormField mergeOptionField(FormField field, Map<String, OptionFieldDefinition> optionFields) {
        if (field.optionBinding() != null || optionFields.isEmpty()) {
            return field;
        }
        OptionFieldDefinition definition = optionFields.get(field.fieldName());
        return definition == null ? field : field.withOptionField(definition);
    }

    private static Map<String, OptionFieldDefinition> optionFields(Class<?> modelClass) {
        if (modelClass == null) {
            return Map.of();
        }
        return OptionFieldResolver.resolve(modelClass).stream()
                .collect(Collectors.toMap(
                        OptionFieldDefinition::fieldName,
                        Function.identity(),
                        (first, ignored) -> first
                ));
    }

    public record Field(String name,
                        String title,
                        FormValueType valueType,
                        FormControlType controlType,
                        boolean required,
                        boolean readOnly,
                        OptionBinding optionBinding,
                        OptionSelectionMode selectionMode,
                        String optionTitleField) {
        public Field {
            optionTitleField = optionTitleField == null || optionTitleField.isBlank() ? null : optionTitleField.trim();
        }

        static Field from(FormField field) {
            return new Field(
                    field.fieldName(),
                    field.title(),
                    field.valueType(),
                    field.controlType(),
                    field.required(),
                    field.readOnly(),
                    field.optionBinding(),
                    field.selectionMode(),
                    field.optionTitleField()
            );
        }
    }
}
