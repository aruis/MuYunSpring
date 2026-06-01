package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.common.formula.FormulaFieldDefinition;
import net.ximatai.muyun.spring.common.formula.FormulaValueType;

import java.util.List;

public final class DynamicFormulaFieldDefinitions {
    private DynamicFormulaFieldDefinitions() {
    }

    public static List<FormulaFieldDefinition> mainFields(EntityDefinition entity) {
        return fields(entity, null);
    }

    public static List<FormulaFieldDefinition> childFields(EntityDefinition entity) {
        return childFields(entity == null ? null : entity.code(), entity);
    }

    public static List<FormulaFieldDefinition> childFields(String relationCode, EntityDefinition entity) {
        return fields(entity, relationCode);
    }

    private static List<FormulaFieldDefinition> fields(EntityDefinition entity, String relationCode) {
        if (entity == null || entity.fields() == null) {
            return List.of();
        }
        String tableKey = relationCode == null || relationCode.isBlank() ? null : relationCode.trim();
        return entity.fields().stream()
                .map(field -> toFormulaField(field, tableKey))
                .toList();
    }

    private static FormulaFieldDefinition toFormulaField(FieldDefinition field, String tableKey) {
        String dataIndex = tableKey == null ? field.code() : tableKey + "." + field.code();
        FormulaFieldDefinition definition = FormulaFieldDefinition.of(dataIndex, valueType(field.type()));
        if (field.isRequired()) {
            definition = definition.asRequired();
        }
        return definition;
    }

    private static FormulaValueType valueType(FieldType fieldType) {
        if (fieldType == null) {
            return FormulaValueType.ANY;
        }
        return switch (fieldType) {
            case STRING -> FormulaValueType.STRING;
            case TEXT -> FormulaValueType.TEXT;
            case INTEGER -> FormulaValueType.INTEGER;
            case LONG -> FormulaValueType.LONG;
            case BOOLEAN -> FormulaValueType.BOOLEAN;
            case TIMESTAMP -> FormulaValueType.TIMESTAMP;
            case DATE -> FormulaValueType.DATE;
            case DECIMAL -> FormulaValueType.DECIMAL;
            case JSON -> FormulaValueType.JSON;
        };
    }
}
