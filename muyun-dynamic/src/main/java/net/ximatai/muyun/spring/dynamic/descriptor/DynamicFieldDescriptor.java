package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.common.option.OptionBinding;
import net.ximatai.muyun.spring.common.option.OptionSelectionMode;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;

public record DynamicFieldDescriptor(
        String fieldName,
        FieldType type,
        String title,
        boolean required,
        boolean unique,
        boolean indexed,
        boolean sortable,
        boolean titleField,
        Integer length,
        Integer precision,
        Integer scale,
        OptionBinding optionBinding,
        OptionSelectionMode selectionMode,
        DynamicReferenceDescriptor reference,
        DynamicFieldQueryDescriptor query,
        String defaultValue,
        String validationRegex,
        boolean copyable,
        boolean writeProtected
) {
    public static DynamicFieldDescriptor from(FieldDefinition field) {
        return new DynamicFieldDescriptor(
                field.fieldName(),
                field.type(),
                field.name(),
                field.isRequired(),
                field.isUnique(),
                field.isIndexed(),
                field.isSortable(),
                field.isTitle(),
                field.length(),
                field.precision(),
                field.scale(),
                field.optionBinding(),
                field.dictionaryBinding() == null ? null : field.dictionaryBinding().selectionMode(),
                null,
                DynamicFieldQueryDescriptor.from(field.queryDefinition()),
                field.behavior().defaultValue(),
                field.behavior().validationRegex(),
                field.behavior().copyable(),
                field.behavior().writeProtected()
        );
    }

    public static DynamicFieldDescriptor from(FieldDefinition field, DynamicReferenceDescriptor reference) {
        DynamicFieldDescriptor descriptor = from(field);
        return new DynamicFieldDescriptor(
                descriptor.fieldName(),
                descriptor.type(),
                descriptor.title(),
                descriptor.required(),
                descriptor.unique(),
                descriptor.indexed(),
                descriptor.sortable(),
                descriptor.titleField(),
                descriptor.length(),
                descriptor.precision(),
                descriptor.scale(),
                descriptor.optionBinding(),
                descriptor.selectionMode(),
                reference,
                descriptor.query(),
                descriptor.defaultValue(),
                descriptor.validationRegex(),
                descriptor.copyable(),
                descriptor.writeProtected()
        );
    }
}
