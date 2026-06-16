package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.common.money.MoneyField;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;

import java.lang.reflect.Field;

public final class StaticMoneyFieldDefinitionCompiler {
    private StaticMoneyFieldDefinitionCompiler() {
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
        FieldMoneyDefinition money = money(field);
        return money.enabled() ? definition.money(money) : definition;
    }

    public static FieldMoneyDefinition money(Field field) {
        if (field == null) {
            return FieldMoneyDefinition.NONE;
        }
        MoneyField annotation = field.getAnnotation(MoneyField.class);
        if (annotation == null) {
            return FieldMoneyDefinition.NONE;
        }
        FieldMoneyMode mode = switch (annotation.currencyMode()) {
            case FIXED -> FieldMoneyMode.FIXED;
            case SELECTABLE -> FieldMoneyMode.SELECTABLE;
        };
        String fixedCurrencyCode = currencyCodeOrNull(annotation.fixedCurrencyCode(), field, "fixedCurrencyCode");
        String currencyFieldName = textOrNull(annotation.currencyFieldName());
        if (mode == FieldMoneyMode.FIXED && fixedCurrencyCode == null) {
            throw invalid(field, "fixed currency mode requires fixedCurrencyCode");
        }
        if (mode == FieldMoneyMode.SELECTABLE && currencyFieldName == null) {
            throw invalid(field, "selectable currency mode requires currencyFieldName");
        }
        if (currencyFieldName != null) {
            requireOptionalFieldName(currencyFieldName, field, "currencyFieldName");
        }
        String baseAmountFieldName = requireFieldName(annotation.baseAmountFieldName(), field, "baseAmountFieldName");
        String defaultCurrencyCode = currencyCodeOrNull(annotation.defaultCurrencyCode(), field, "defaultCurrencyCode");
        String baseCurrencyCode = currencyCodeOrNull(annotation.baseCurrencyCode(), field, "baseCurrencyCode");
        String rateTypeCode = requireRateTypeCode(annotation.rateTypeCode(), field, "rateTypeCode");
        String rateDateFieldName = textOrNull(annotation.rateDateFieldName());
        if (rateDateFieldName != null) {
            requireOptionalFieldName(rateDateFieldName, field, "rateDateFieldName");
        }
        String exchangeRateFieldName = textOrNull(annotation.exchangeRateFieldName());
        if (exchangeRateFieldName != null) {
            requireOptionalFieldName(exchangeRateFieldName, field, "exchangeRateFieldName");
        }
        return new FieldMoneyDefinition(
                mode,
                fixedCurrencyCode,
                defaultCurrencyCode,
                currencyFieldName,
                baseAmountFieldName,
                baseCurrencyCode,
                rateTypeCode,
                rateDateFieldName,
                exchangeRateFieldName,
                annotation.currencyRequired()
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

    private static void requireOptionalFieldName(String value, Field field, String name) {
        try {
            PlatformNameRules.requireFieldName(value, "money " + name);
        } catch (RuntimeException e) {
            throw invalid(field, e.getMessage());
        }
    }

    private static String requireFieldName(String value, Field field, String name) {
        String text = textOrNull(value);
        if (text == null) {
            throw invalid(field, "money " + name + " must not be blank");
        }
        try {
            return PlatformNameRules.requireFieldName(text, "money " + name);
        } catch (RuntimeException e) {
            throw invalid(field, e.getMessage());
        }
    }

    private static String currencyCodeOrNull(String value, Field field, String name) {
        String text = textOrNull(value);
        if (text == null) {
            return null;
        }
        String code = text.toUpperCase();
        if (!code.matches("[A-Z]{3}")) {
            throw invalid(field, "money " + name + " must be ISO 4217 alpha-3 code: " + value);
        }
        return code;
    }

    private static String requireRateTypeCode(String value, Field field, String name) {
        String text = textOrNull(value);
        if (text == null) {
            throw invalid(field, "money " + name + " must not be blank");
        }
        String code = text.toUpperCase();
        if (!code.matches("[A-Z][A-Z0-9_]{0,63}")) {
            throw invalid(field, "money " + name + " must use upper snake code: " + value);
        }
        return code;
    }

    private static IllegalArgumentException invalid(Field field, String message) {
        return new IllegalArgumentException("invalid static money field "
                + field.getDeclaringClass().getName() + "." + field.getName() + ": " + message);
    }

    private static String textOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
