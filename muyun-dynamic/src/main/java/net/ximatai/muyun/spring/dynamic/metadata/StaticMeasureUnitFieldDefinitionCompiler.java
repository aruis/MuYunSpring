package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.common.measure.MeasureUnitField;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;

import java.lang.reflect.Field;

public final class StaticMeasureUnitFieldDefinitionCompiler {
    private StaticMeasureUnitFieldDefinitionCompiler() {
    }

    public static FieldDefinition compile(FieldDefinition definition, Class<?> modelClass) {
        if (definition == null || modelClass == null) {
            return definition;
        }
        Field field = findField(modelClass, definition.fieldName());
        return field == null ? definition : compile(definition, field);
    }

    public static FieldDefinition compile(FieldDefinition definition, Field field) {
        if (definition == null || field == null) {
            return definition;
        }
        FieldMeasureUnitDefinition measureUnit = measureUnit(field);
        return measureUnit.enabled() ? definition.measureUnit(measureUnit) : definition;
    }

    public static FieldMeasureUnitDefinition measureUnit(Field field) {
        if (field == null) {
            return FieldMeasureUnitDefinition.NONE;
        }
        MeasureUnitField annotation = field.getAnnotation(MeasureUnitField.class);
        if (annotation == null) {
            return FieldMeasureUnitDefinition.NONE;
        }
        String categoryAlias = requireIdentifier(annotation.categoryAlias(), field, "categoryAlias");
        FieldMeasureUnitMode mode = switch (annotation.mode()) {
            case FIXED -> FieldMeasureUnitMode.FIXED;
            case SELECTABLE -> FieldMeasureUnitMode.SELECTABLE;
        };
        String fixedUnitCode = textOrNull(annotation.fixedUnitCode());
        String unitFieldName = textOrNull(annotation.unitFieldName());
        if (mode == FieldMeasureUnitMode.FIXED && fixedUnitCode == null) {
            throw invalid(field, "fixed unit mode requires fixedUnitCode");
        }
        if (mode == FieldMeasureUnitMode.SELECTABLE && unitFieldName == null) {
            throw invalid(field, "selectable unit mode requires unitFieldName");
        }
        if (unitFieldName != null) {
            requireOptionalFieldName(unitFieldName, field, "unitFieldName");
        }
        String baseValueFieldName = requireFieldName(annotation.baseValueFieldName(), field, "baseValueFieldName");
        String baseUnitCategoryAlias = textOrNull(annotation.baseUnitCategoryAlias());
        if (baseUnitCategoryAlias != null) {
            requireOptionalIdentifier(baseUnitCategoryAlias, field, "baseUnitCategoryAlias");
        }
        String baseUnitCode = requireIdentifier(annotation.baseUnitCode(), field, "baseUnitCode");
        String defaultUnitCode = textOrNull(annotation.defaultUnitCode());
        if (defaultUnitCode != null) {
            requireOptionalIdentifier(defaultUnitCode, field, "defaultUnitCode");
        }
        String conversionScopeFieldName = textOrNull(annotation.conversionScopeFieldName());
        if (conversionScopeFieldName != null) {
            requireOptionalFieldName(conversionScopeFieldName, field, "conversionScopeFieldName");
        }
        FieldMeasureUnitConversionMode conversionMode = switch (annotation.conversionMode()) {
            case LINEAR -> FieldMeasureUnitConversionMode.LINEAR;
            case BUSINESS_RULE -> FieldMeasureUnitConversionMode.BUSINESS_RULE;
        };
        return new FieldMeasureUnitDefinition(
                categoryAlias,
                mode,
                fixedUnitCode,
                defaultUnitCode,
                unitFieldName,
                baseValueFieldName,
                baseUnitCategoryAlias == null ? categoryAlias : baseUnitCategoryAlias,
                baseUnitCode,
                conversionMode,
                conversionScopeFieldName,
                annotation.unitRequired()
        );
    }

    private static Field findField(Class<?> modelClass, String fieldName) {
        Class<?> current = modelClass;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static void requireOptionalIdentifier(String value, Field field, String name) {
        try {
            PlatformNameRules.requireIdentifier(value, "measure " + name);
        } catch (RuntimeException e) {
            throw invalid(field, e.getMessage());
        }
    }

    private static void requireOptionalFieldName(String value, Field field, String name) {
        try {
            PlatformNameRules.requireFieldName(value, "measure " + name);
        } catch (RuntimeException e) {
            throw invalid(field, e.getMessage());
        }
    }

    private static String requireIdentifier(String value, Field field, String name) {
        String text = textOrNull(value);
        if (text == null) {
            throw invalid(field, "measure " + name + " must not be blank");
        }
        try {
            return PlatformNameRules.requireIdentifier(text, "measure " + name);
        } catch (RuntimeException e) {
            throw invalid(field, e.getMessage());
        }
    }

    private static String requireFieldName(String value, Field field, String name) {
        String text = textOrNull(value);
        if (text == null) {
            throw invalid(field, "measure " + name + " must not be blank");
        }
        try {
            return PlatformNameRules.requireFieldName(text, "measure " + name);
        } catch (RuntimeException e) {
            throw invalid(field, e.getMessage());
        }
    }

    private static IllegalArgumentException invalid(Field field, String message) {
        return new IllegalArgumentException("invalid static measure unit field "
                + field.getDeclaringClass().getName() + "." + field.getName() + ": " + message);
    }

    private static String textOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
