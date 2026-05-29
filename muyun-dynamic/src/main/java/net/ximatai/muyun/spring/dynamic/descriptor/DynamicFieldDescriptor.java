package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.common.option.OptionBinding;
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
        DynamicFieldQueryDescriptor query
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
                DynamicFieldQueryDescriptor.from(field.queryDefinition())
        );
    }
}
