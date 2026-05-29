package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;

@FunctionalInterface
public interface DynamicFieldValueValidator {
    DynamicFieldValueValidator NONE = (moduleAlias, entity, field, value) -> {
    };

    void validate(String moduleAlias, EntityDefinition entity, FieldDefinition field, Object value);
}
